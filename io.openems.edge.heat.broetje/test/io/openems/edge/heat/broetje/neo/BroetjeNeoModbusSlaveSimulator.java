package io.openems.edge.heat.broetje.neo;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.ProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

/**
 * Simulates the NEO-RKM Modbus register map of a Broetje BLW NEO heat pump, for
 * dry tests (Trockentest) of {@code Heat.Broetje.Neo} against a local Edge.
 *
 * <p>
 * Serves the complete NEO register map on TCP port 1502, Unit-ID 1: input
 * registers (FC4) 10-75 with temperatures, status flags, energies and power;
 * holding registers (FC3 read / FC6 write with readback) 100 (operating mode),
 * 125 (power setpoint), 149/150 (grid operator lock).
 *
 * <p>
 * Start via {@link #main(String[])}; see
 * https://openems.github.io/openems.io/openems/latest/edge/implement.html
 */
public class BroetjeNeoModbusSlaveSimulator {

	public static final int PORT = 1502;
	private static final int UNIT_ID = 1;
	private static final int CONCURRENT_CONNECTIONS = 5;

	/**
	 * Start the {@link BroetjeNeoModbusSlaveSimulator}.
	 *
	 * @param args the command line arguments
	 * @throws ModbusException on error
	 */
	public static void main(String[] args) throws ModbusException {
		// Start Modbus/TCP slave
		var image = createProcessImage();
		var slave = ModbusSlaveFactory.createTCPSlave(PORT, CONCURRENT_CONNECTIONS);
		slave.addProcessImage(UNIT_ID, image);
		slave.open();

		System.out.println("Broetje NEO Modbus slave simulator started. Port [" + PORT + "] Unit-ID [" + UNIT_ID + "]");

		// Stop Modbus/TCP slave on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				ModbusSlaveFactory.close();
				System.out.println("Broetje NEO Modbus slave simulator stopped.");
			}
		});

		// Wait forever
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates the NEO-RKM register map as a {@link ProcessImage}.
	 *
	 * @return the {@link ProcessImage}
	 */
	public static ProcessImage createProcessImage() {
		var image = new SimpleProcessImage(UNIT_ID);

		// Input registers (FC4), temperatures in 0.1 °C - registers 10-14
		image.addInputRegister(10, new SimpleRegister(50)); // outdoor temperature
		image.addInputRegister(11, new SimpleRegister(480)); // domestic hot water temperature
		image.addInputRegister(12, new SimpleRegister(350)); // flow temperature
		image.addInputRegister(13, new SimpleRegister(300)); // return temperature
		image.addInputRegister(14, new SimpleRegister(420)); // buffer/storage temperature

		// Input registers 15-24: optional values, plausible here
		image.addInputRegister(15, new SimpleRegister(280)); // source flow temperature
		image.addInputRegister(16, new SimpleRegister(255)); // source return temperature
		image.addInputRegister(17, new SimpleRegister(85)); // suction gas temperature
		image.addInputRegister(18, new SimpleRegister(40)); // evaporation temperature
		image.addInputRegister(19, new SimpleRegister(460)); // condensation temperature
		image.addInputRegister(20, new SimpleRegister(650)); // hot gas temperature
		image.addInputRegister(21, new SimpleRegister(28)); // pressure 1
		image.addInputRegister(22, new SimpleRegister(190)); // pressure 2
		image.addInputRegister(23, new SimpleRegister(1)); // pump 1 active
		image.addInputRegister(24, new SimpleRegister(0)); // pump 2 inactive

		// Input registers 25/26/27: running, no fault, no defrost
		image.addInputRegister(25, new SimpleRegister(1)); // compressor running
		image.addInputRegister(26, new SimpleRegister(0)); // no device fault
		image.addInputRegister(27, new SimpleRegister(0)); // no defrost (4-way valve)

		// Input registers 28-41, like the HeatBroetjeNeoImplTest scenario
		image.addInputRegister(28, new SimpleRegister(0)); // volume flow
		image.addInputRegister(29, new SimpleRegister(450)); // compressor speed [‰]
		image.addInputRegister(30, new SimpleRegister(35)); // COP [0.1] = 3.5, consistent with 1500 W / 5.2 kW
		image.addInputRegister(31, new SimpleRegister(0)); // fresh water temperature
		image.addInputRegister(32, new SimpleRegister(1)); // grid operator release
		image.addInputRegister(33, new SimpleRegister(0)); // delayed outdoor temperature
		image.addInputRegister(34, new SimpleRegister(450)); // return/storage setpoint [0.1 °C]
		image.addInputRegister(35, new SimpleRegister(0)); // mixer circuit setpoint 1
		image.addInputRegister(36, new SimpleRegister(0)); // mixer circuit setpoint 2
		image.addInputRegister(37, new SimpleRegister(0));
		image.addInputRegister(38, new SimpleRegister(0));
		image.addInputRegister(39, new SimpleRegister(0));
		image.addInputRegister(40, new SimpleRegister(0)); // expansion valve
		image.addInputRegister(41, new SimpleRegister(20)); // demand: space heating

		// Input registers 60-75: energies as UINT32 doublewords, power, thermal power
		image.addInputRegister(60, new SimpleRegister(0)); // thermal energy heating [kWh] high word
		image.addInputRegister(61, new SimpleRegister(10)); // ... low word
		image.addInputRegister(62, new SimpleRegister(0)); // electric energy heating high word
		image.addInputRegister(63, new SimpleRegister(20)); // ... low word
		image.addInputRegister(64, new SimpleRegister(0)); // thermal energy DHWS high word
		image.addInputRegister(65, new SimpleRegister(5)); // ... low word
		image.addInputRegister(66, new SimpleRegister(0)); // electric energy DHWS high word
		image.addInputRegister(67, new SimpleRegister(6)); // ... low word
		image.addInputRegister(68, new SimpleRegister(0)); // total electric energy high word
		image.addInputRegister(69, new SimpleRegister(1234)); // ... low word [kWh]
		image.addInputRegister(70, new SimpleRegister(0)); // electric power [W] high word
		image.addInputRegister(71, new SimpleRegister(1500)); // ... low word
		image.addInputRegister(72, new SimpleRegister(0)); // total thermal energy high word
		image.addInputRegister(73, new SimpleRegister(7)); // ... low word [kWh]
		image.addInputRegister(74, new SimpleRegister(0)); // thermal power [0.1 kW] high word
		image.addInputRegister(75, new SimpleRegister(52)); // ... low word = 5.2 kW

		// Holding registers (FC3 read / FC6 write with readback)
		image.addRegister(100, new SimpleRegister(1)); // operating mode: automatic
		image.addRegister(125, new SimpleRegister(0)); // power setpoint [W], writable
		image.addRegister(149, new SimpleRegister(0)); // grid operator release value, writable
		image.addRegister(150, new SimpleRegister(0)); // grid operator release activate, writable
		return image;
	}

	private BroetjeNeoModbusSlaveSimulator() {
	}
}
