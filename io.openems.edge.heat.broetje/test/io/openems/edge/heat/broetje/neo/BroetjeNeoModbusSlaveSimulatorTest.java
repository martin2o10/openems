package io.openems.edge.heat.broetje.neo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BroetjeNeoModbusSlaveSimulatorTest {

	@Test
	void testRegisterMap() {
		var image = BroetjeNeoModbusSlaveSimulator.createProcessImage();

		// temperatures 10-14 [0.1 °C]
		assertEquals(50, image.getInputRegister(10).getValue());
		assertEquals(480, image.getInputRegister(11).getValue());
		assertEquals(350, image.getInputRegister(12).getValue());
		assertEquals(300, image.getInputRegister(13).getValue());
		assertEquals(420, image.getInputRegister(14).getValue());

		// status flags 25/26/27: running, no fault, no defrost
		assertEquals(1, image.getInputRegister(25).getValue());
		assertEquals(0, image.getInputRegister(26).getValue());
		assertEquals(0, image.getInputRegister(27).getValue());

		// demand 41 = 20 (space heating)
		assertEquals(20, image.getInputRegister(41).getValue());

		// electric power 70/71 = 1500 W, thermal power 74/75 = 52 (5.2 kW)
		assertEquals(0, image.getInputRegister(70).getValue());
		assertEquals(1500, image.getInputRegister(71).getValue());
		assertEquals(0, image.getInputRegister(74).getValue());
		assertEquals(52, image.getInputRegister(75).getValue());

		// holding registers: 100 = 1 (automatic); 125/149/150 writable, default 0
		assertEquals(1, image.getRegister(100).getValue());
		assertEquals(0, image.getRegister(125).getValue());
		assertEquals(0, image.getRegister(149).getValue());
		assertEquals(0, image.getRegister(150).getValue());

		// operating hours 42/44 as doublewords
		assertEquals(0, image.getInputRegister(42).getValue());
		assertEquals(1234, image.getInputRegister(43).getValue());
		assertEquals(0, image.getInputRegister(44).getValue());
		assertEquals(56, image.getInputRegister(45).getValue());

		// optional holding registers ("Kür")
		assertEquals(1010, image.getRegister(101).getValue());
		assertEquals(1060, image.getRegister(106).getValue());
		assertEquals(0, image.getRegister(117).getValue());
		assertEquals(450, image.getRegister(126).getValue());
		assertEquals(0, image.getRegister(127).getValue());
		assertEquals(0, image.getRegister(129).getValue());
		assertEquals(0, image.getRegister(134).getValue());
		assertEquals(0, image.getRegister(147).getValue());
		assertEquals(0, image.getRegister(148).getValue());
	}
}
