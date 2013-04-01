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

package com.example.framjackck;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.example.framjackck.ApplicationInterface;
import com.example.framjackck.FramingEngine;
import com.example.framjackck.FramingEngine.IncomingPacketListener;
import com.example.framjackck.FramingEngine.OutgoingByteListener;
import com.example.framjackck.core.OnByteSentListener;
import com.example.framjackck.core.OnBytesAvailableListener;
import com.example.framjackck.core.SerialDecoder;

public class ApplicationInterface {
	private SerialDecoder _serialDecoder;
	private FramingEngine _framer;
	
	//private final double _scalingVoltage = 1.8;
	private final double _vccHijack = 2.76; //measure and set hijack's Vcc
	
	private boolean[] _digitalOutputState = new boolean[] {false, false, false, false};
	private boolean[] _digitalInputState = new boolean[] {false, false};
	private double[] _analogInputState = new double[] {0.0, 0.0, 0.0};
	//private double[] _tempreading = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	//private double[] _humreading = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	//private int _index = 0;
	
	private boolean _isConnected = false;
	private boolean _hasUpdated = false;
	
	private int _pendingTransmitBytes = 0;
	
	private Timer _watchdogConnectionTimer;
	private UpdateListener _listener = null;
	
	//private double previoustemp = 100.00, previoushum = 100.00;
	
	public ApplicationInterface() {
		_framer = new FramingEngine();
		_serialDecoder = new SerialDecoder();
		
		
		_serialDecoder.registerBytesAvailableListener(_bytesAvailableListener);
		_serialDecoder.registerByteSentListener(_byteSentListener);
		_framer.registerIncomingPacketListener(_incomingPacketListener);
		_framer.registerOutgoingByteListener(_outgoingByteListener);	
		
		int[] toSend = encode();
		for (int i = 0; i < toSend.length; i++) {
			_framer.transmitByte(toSend[i]);
		}
		_framer.transmitEnd();
		
	}
	
	///////////////////////////////////////////////
	// Listeners
	///////////////////////////////////////////////	
	private OutgoingByteListener _outgoingByteListener = new OutgoingByteListener() {
		public void OutgoingByteTransmit(int[] outgoingRaw) {
			synchronized (ApplicationInterface.this) {
				_pendingTransmitBytes += outgoingRaw.length;
			}
			
			for (int i = 0; i < outgoingRaw.length; i++) {
				_serialDecoder.sendByte(outgoingRaw[i]);
			}
		}
	};
	
	private IncomingPacketListener _incomingPacketListener = new IncomingPacketListener() {
		public void IncomingPacketReceive(int[] packet) {
			for (int i = 0; i < packet.length; i++) {
				System.out.print(Integer.toHexString(packet[i]) + " ");
			}
			System.out.println();
			decodeAndUpdate(packet);
		}
	};
	
	private OnByteSentListener _byteSentListener = new OnByteSentListener() {
		public void onByteSent() {
			synchronized (ApplicationInterface.this) {
				_pendingTransmitBytes--;
				if (_pendingTransmitBytes == 0) {
					int[] toSend = encode();
					for (int i = 0; i < toSend.length; i++) {
						_framer.transmitByte(toSend[i]);
					}
					_framer.transmitEnd();
				}
			}	
		}
	};
	
	private OnBytesAvailableListener _bytesAvailableListener = new OnBytesAvailableListener() {
		public void onBytesAvailable(int count) {
			while(count > 0) {
				int byteVal = _serialDecoder.readByte();
				//System.out.println("Received: " + byteVal);
				_framer.receiveByte(byteVal);
				count--;
			}
		}
	};
	
	///////////////////////////////////////////////
	// Building the Byte Stream
	///////////////////////////////////////////////	
	private void decodeAndUpdate(int[] packet) {
		if (packet.length != 9) {
			return;
		}
		
		synchronized (this) {
			
			_hasUpdated = true;
			
			for (int i = 0; i < 2; i++) {
				_digitalInputState[i] = ((packet[0] >> i) & 0x01) == 1;
			}
			
			int[] rawAdcValues = new int[4];
			double unitADC = _vccHijack/4095;
			
			rawAdcValues[1] = ((packet[2 + 1*2] & 0xFF) << 8) | (packet[1 + 1*2] & 0xFF);
			rawAdcValues[2] = (((packet[2 + 2*2] & 0xFF) << 8) | (packet[1 + 2*2] & 0xFF));
			rawAdcValues[3] = ((packet[2 + 3*2] & 0xFF) << 8) | (packet[1 + 3*2] & 0xFF);			
				
			_analogInputState[1] = unitADC*rawAdcValues[2];
			
			/*if (_index < 10) {
				_tempreading[_index] = -46.85 + 175.72 * (rawAdcValues[1]/(Math.pow(2,16))) ;
				_humreading[_index] = -6 + 125 * (rawAdcValues[3]/(Math.pow(2,16)));
				if (_humreading[_index] < 0 || _humreading[_index] > 100 || _tempreading[_index] == -46.85) { 
					_humreading[_index] = 0;
					_index--;
				}
				_index++;
			}
			
			else {
				
				//Get Average of 10 readings
				double avgtemp = (_tempreading[0] + _tempreading[1] + _tempreading[2]
									+ _tempreading[3] + _tempreading[4] + _tempreading[5]
									+ _tempreading[6] + _tempreading[7] + _tempreading[8] + _tempreading[9])/10;
				double avghum = (_humreading[0] + _humreading[1] + _humreading[2]
									+ _humreading[3] + _humreading[4] + _humreading[5]
									+ _humreading[6] + _humreading[7] + _humreading[8] + _humreading[9])/10;
				
				//Check if average is within acceptable range
				if ( 		(avgtemp < (_tempreading[0] + 1) && avgtemp > (_tempreading[0] - 1)) 
						&&	(avgtemp < (_tempreading[1] + 1) && avgtemp > (_tempreading[1] - 1))
						&&	(avgtemp < (_tempreading[2] + 1) && avgtemp > (_tempreading[2] - 1))
						&&	(avgtemp < (_tempreading[3] + 1) && avgtemp > (_tempreading[3] - 1))
						&&	(avgtemp < (_tempreading[4] + 1) && avgtemp > (_tempreading[4] - 1))
						&&	(avgtemp < (_tempreading[5] + 1) && avgtemp > (_tempreading[5] - 1))
						&&	(avgtemp < (_tempreading[6] + 1) && avgtemp > (_tempreading[6] - 1))
						&&	(avgtemp < (_tempreading[7] + 1) && avgtemp > (_tempreading[7] - 1))
						&&	(avgtemp < (_tempreading[8] + 1) && avgtemp > (_tempreading[8] - 1))
						&&	(avgtemp < (_tempreading[9] + 1) && avgtemp > (_tempreading[9] - 1)) )
					{
						//If first reading or no data received from Hijack
						if (previoustemp == 100.00 || previoustemp == -46.85) {
								previoustemp = avgtemp;
								_analogInputState[0] = avgtemp;
						}
						else {
							if (avgtemp < (previoustemp + 2.5) && avgtemp > (previoustemp - 2.5))
								_analogInputState[0] = avgtemp;
								Log.v ("TH", "T= " + _analogInputState[0]);
						}
					}
				else _analogInputState[0] = _analogInputState[0];
				
				if ( 		(avghum < (_humreading[0] + 2) && avghum > (_humreading[0] - 2)) 
						&&	(avghum < (_humreading[1] + 2) && avghum > (_humreading[1] - 2))
						&&	(avghum < (_humreading[2] + 2) && avghum > (_humreading[2] - 2))
						&&	(avghum < (_humreading[3] + 2) && avghum > (_humreading[3] - 2))
						&&	(avghum < (_humreading[4] + 2) && avghum > (_humreading[4] - 2)) 
						&&	(avghum < (_humreading[5] + 2) && avghum > (_humreading[5] - 2))
						&&	(avghum < (_humreading[6] + 2) && avghum > (_humreading[6] - 2)) 
						&&	(avghum < (_humreading[7] + 2) && avghum > (_humreading[7] - 2))
						&&	(avghum < (_humreading[8] + 2) && avghum > (_humreading[8] - 2)) 
						&&	(avghum < (_humreading[9] + 2) && avghum > (_humreading[9] - 2)) )
					{
					
						if (previoushum == 100.00 || previoushum < 0) {
								previoushum = avghum;
								_analogInputState[0] = avgtemp;
						}
						else {
							if (avghum < (previoushum + 2.5) && avghum > (previoushum - 2.5))
								_analogInputState[2] = avghum;
								Log.v ("TH", "H= " + _analogInputState[2]);
						}
					}
				else _analogInputState[2] = _analogInputState[2];
				
				_index = 0;
			}*/
			
			_analogInputState[2] = -6 + 125 * (rawAdcValues[3]/(Math.pow(2,16)));
			double temp = -46.85 + 175.72 * (rawAdcValues[1]/(Math.pow(2,16)));
			if (temp<-40 || temp>45) _analogInputState[0]=_analogInputState[0];
			else _analogInputState[0] = temp;
			
			//if (_analogInputState[0] < 0) _analogInputState[0] = 0;
			//if (_analogInputState[0] > 100) _analogInputState[0] = 100;
			
		}
		
		OnUpdateListener();
	}
	
	private int[] encode() {
		int retValue = 0;
		for (int i = 0; i < 4; i++) {
			if (_digitalOutputState[i]) {
				retValue |= (1 << i);
			}
		}
		
		return new int[] {retValue};
	}
	
	///////////////////////////////////////////////
	// Start/Stop Stuff =)
	///////////////////////////////////////////////	
	
	public void start() {
		_serialDecoder.start();
		_watchdogConnectionTimer = new Timer();
		_watchdogConnectionTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				synchronized (ApplicationInterface.this) {
					_isConnected = _hasUpdated;
					_hasUpdated = false;
				}
				OnUpdateListener();
			}
		}, 1000, 1000);
	}
	
	public void stop() {
		_watchdogConnectionTimer.cancel();
		_watchdogConnectionTimer = null;
		_serialDecoder.stop();
		
		_isConnected = false;
		OnUpdateListener();
	}

	///////////////////////////////////////////////
	// Public Interface
	///////////////////////////////////////////////	
	
	public synchronized void setOutput(int outputId, boolean isHigh) {
		_digitalOutputState[outputId] = isHigh;
	}
	
	public synchronized void registerOnUpdateListener(UpdateListener listener) {
		_listener = listener;
	}
	
	public synchronized boolean getDigitalInput(int inputId) {
		return _digitalInputState[inputId];
	}
	
	public synchronized double getAnalogInput(int inputId) {
		return _analogInputState[inputId - 1];
	}
	
	public synchronized double getTemperature() {
		return _analogInputState[0];
	}
	
	public synchronized boolean getIsConnected() {
		return _isConnected;
	}
	
	public interface UpdateListener {
		public abstract void Update();
	}
	
	///////////////////////////////////////////////
	// Helpers
	///////////////////////////////////////////////
	
	// NOT THREADSAFE
	public void OnUpdateListener() {
		if (_listener != null) {
			_listener.Update();
		}
	}
}
