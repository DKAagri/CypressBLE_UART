# CypressBLE_UART

Aim : To write charecteristic to BLE device from android app. The vallues should be written in HEX and send to BLE device.

Process :
1. Connect Cypress IC to LED , power and ground 
2. Attach UART through USB (adafruit) for reading values on desktop IDE - for debugging
3. Install android studio and open the code in debugging mode 
4. Install Capsense module on IC
5. Run the android program , find and pair to the device 
6. Use the service capsense RGB to change color and intensity
7. Read values on IDE
