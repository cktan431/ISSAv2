/*
 *  This file is part of hijack-infinity.
 *
 *  hijack-infinity is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  hijack-infinity is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with hijack-infinity.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <inttypes.h>
#include <stdio.h>

#include "pal.h"
#include "codingStateMachine.h"
#include "framingEngine.h"

uint8_t outMessage[] = {
	0xFF, 		// Make the first 2 bits of this
				// equal to the state of two input pins.
	0x3F, 0xFF, // Analog In 1
	0x3F, 0xFF, // Analog In 2
	0x3F, 0xFF, // Analog In 3 (temperature)
	0x2F, 0xFF  // Analog In 4 (potentially a v_ref)
};

uint16_t sample1 = 0;
uint16_t sample2 = 0;
uint16_t temp_ext = 0;
uint16_t vcc = 0;

uint16_t temp_buf[100] = {0};
uint8_t buf_idx = 0;

char RXData[6] = {0};
unsigned int humidity = 0;
unsigned int xmit, rcv = 0;
uint16_t i = 0x0000;

void periodicTimerFn (void) {

	uint8_t ats;
	ats = csm_advanceTransmitState();

	if (ats) {
		pal_setDigitalGpio(pal_gpio_mic, 1);
	} else {
		pal_setDigitalGpio(pal_gpio_mic, 0);
	}

	csm_finishAdvanceTransmitState();
}

void captureTimerFn(uint16_t elapsedTime, uint8_t isHigh) {
	struct csm_timer_struct timingData;
	timingData.elapsedTime = elapsedTime;
	timingData.signal = !isHigh;
	csm_receiveTiming(&timingData);	
}

void packetReceivedCallback(uint8_t * buf, uint8_t len) {
	if (len == 1) {
		pal_setDigitalGpio(pal_gpio_led, 1);
		pal_setDigitalGpio(pal_gpio_dout1, ((buf[0] >> 0) & 0x01));
		pal_setDigitalGpio(pal_gpio_dout2, ((buf[0] >> 1) & 0x01));
		pal_setDigitalGpio(pal_gpio_dout3, ((buf[0] >> 2) & 0x01));
		pal_setDigitalGpio(pal_gpio_dout4, ((buf[0] >> 3) & 0x01));
	}
}

void packetSentCallback(void) {
	fe_writeTxBuffer(outMessage, 9);
}

void updateAnalogOutputBuffer(void) {

	outMessage[1] = vcc & 0xFF;
	outMessage[2] = (vcc >> 8) & 0xFF;
	outMessage[3] = temp_ext & 0xFF;
	outMessage[4] = (temp_ext >> 8) & 0xFF;
	outMessage[5] = sample1 & 0xFF;
	outMessage[6] = (sample1 >> 8) & 0xFF;
	outMessage[7] = sample2 & 0xFF;
	outMessage[8] = (sample2 >> 8) & 0xFF;

}

void updateDigitalOutputBuffer(void) {
	outMessage[0] = 0x03 & (P4IN>>5);
}

void initializeSystem(void) {
	pal_init();

	// Initialize the transition-edge-length
	// manchester decoder.
	csm_init();
	csm_registerReceiveByte(fe_handleByteReceived);
	csm_registerTransmitByte(fe_handleByteSent);

	// Initialize the framing engine to process
	// the raw byte stream.
	fe_init();
	fe_registerPacketReceivedCb(packetReceivedCallback);
	fe_registerPacketSentCb(packetSentCallback);
	fe_registerByteSender(csm_sendByte);

	pal_registerPeriodicTimerCb(periodicTimerFn);
	pal_registerCaptureTimerCb(captureTimerFn);

	// Start the interrupt-driven timers.
	pal_startTimers();

	// Start the transmit callback-driven
	// loop
	fe_writeTxBuffer(outMessage, 9);
	fe_startSending();
	
}

// This is the main loop. It's not very
// power efficient right now, it should
// do some microcontroller sleep commands
// and use wakeups.
int main () {
	initializeSystem();
	
	WDTCTL = WDTPW + WDTHOLD;						// Stop WDT
	
	/////////////////////////////
	/////////// A D C ///////////
	/////////////////////////////
	ADC12CTL0 = SHT0_7 + ADC12ON;					// Set sampling time, turn on ADC12
	ADC12CTL1 = CSTARTADD_0 + SHP + CONSEQ_0 + ADC12DIV_7 + ADC12SSEL_1;		// Conversion Start Address, Sample-hold Pulse Mode
																				// Single Chn Single Conv, 8x Clock Divide, ADC CLK = ACLK
	ADC12MCTL0 = INCH_6;							// INPUT CHANNEL A6, Reference = AVdd and AVss
	ADC12IE = 0x01;                         		// Enable interrupt A6
	ADC12CTL0 |= ENC;								// Conversion enabled
	
	/////////////////////////////
	/////////// I 2 C ///////////
	/////////////////////////////
	U0CTL |= I2C + SYNC;						// Switch USART0 to I2C mode
	U0CTL &= ~I2CEN;							// Recommended I2C init procedure
	I2CTCTL = I2CSSEL_1;						// ACLK
	I2CPSC = 0x03;								// Divider = 4

	I2CSA = 0x40;								// Slave address	
	I2CIE |= TXRDYIE+RXRDYIE+ARDYIE+NACKIE;
	U0CTL |= I2CEN;								// Enable I2C, 7 bit addr,
	
	I2CNDAT = 0x01;								// Send 1 Byte
	U0CTL |= MST;
	I2CTCTL |= I2CTRX + I2CSTT;     		 	// Transmit, Restart
	
	while(1) {
		updateAnalogOutputBuffer();				// Update data packet
		
		ADC12CTL0 |= ADC12SC;					// Start ADC Conversion
		
		//_BIS_SR(CPUOFF + GIE);				//LPM0
		_BIS_SR(LPM3_bits + GIE);				//LPM3
		
		i++;
		
		if (i == 0x0FFF) {
			I2CNDAT = 0x01;								// Send 1 Byte
			U0CTL |= MST;
			I2CTCTL |= I2CTRX + I2CSTT;     		 	// Transmit, Restart
			i=0x0000;
		}

	}
    return 0;
}

#pragma vector=ADC12_VECTOR
__interrupt void ADC12_ISR (void)
{
	sample1 = ADC12MEM0;
	
	sample2 = RXData[0];
	sample2 = sample2 << 8;
	sample2 |= (RXData[1] & 0xFC);
	
	temp_ext = RXData[3];
	temp_ext = temp_ext << 8;
	sample2 |= (RXData[4] & 0xFC);
	
    _BIC_SR_IRQ(LPM3_bits);
}

// USART0 I2C interrupt service routine
#pragma vector=USART0TX_VECTOR
__interrupt void I2C_IV (void)
{
  switch( I2CIV )
  {
  case 0x02: break;						// ALIFG: n/a
  case 0x04: break;						// NACKIFG: n/a
  case 0x06: break;						// OAIFG: n/a
  case 0x08: 	if (xmit == 0) {
					I2CNDAT = 0x03;
					I2CTCTL &= ~I2CTRX;
					I2CTCTL |= I2CSTT;
				}
				if (rcv == 3) {
					I2CTCTL |= I2CSTP;
					while (I2CBB & I2CDCTL);
					rcv = 0;
					P1OUT = 0x08;
					_BIC_SR_IRQ(LPM3_bits);
				}
				break;						// ARDYIFG: n/a
  case 0x0A: 	RXData[rcv+humidity] = I2CDRB; 	// Total 3 bytes received, 2 bytes data + 1 byte checksum
				rcv++;							// hence temp data is store 3 bytes away
				xmit = 1;
				break;						// RXRDYIFG: n/a
  case 0x0C: 	if (humidity == 0) {
					humidity = 3;
					I2CDRB = 0xE5;
				}
				else {
					humidity = 0;
					I2CDRB = 0xE3;
				}
				xmit = 0;
				P1OUT = 0x04;
				break;						// TXRDYIFG: n/a
  case 0x0E: break;						// GCIFG: n/a
  case 0x10: break;						// STTIFG: n/a
 }
}