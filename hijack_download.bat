@echo off
cd..
D:
cd D:\cygwin\home\ChoonKiat\tos-bsl-win
tos-bsl --comport=11 --swap-reset-test --invert-reset --invert-test -r -e -I -p -U out.hex
pause
