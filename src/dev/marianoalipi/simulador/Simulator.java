package dev.marianoalipi.simulador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Simulator extends JFrame {

	private static final long serialVersionUID = 1L;
	static Simulator currentSim;

	final int MEMSIZE = 1000;
	// Arreglos con los códigos de operación.
	//  				00     01     02      03     04     05    06     07     08
	String codes[] = {"NOP", "CLA", "LDA", "STA", "ADD", "SUB", "NEG", "JMP", "HLT"};
	//Arreglo de la memoria del simulador.
	String data[] = new String[MEMSIZE];
	//Opciones.
	boolean bContinue;
	//Valor del PC inicial
	int PC = 0, PCprev;
	//Otros registros
	String MDR, AC, MAR, IR;
	// Espacio entre número de celda y contenido.
	final String SPC = "     ";
	//Duración del intervalo de ejecución de las microoperaciones.
	int secs = 3;

	JSpinner addressSN;
	JTextField contentTF;
	JButton executeB;
	JSlider intervalSL;
	JList<String> dataLS;
	JComboBox<String> typeCB;
	JTextField irTF = null, pcTF = null, marTF = null, mdrTF = null, acTF = null;

	ActionListener actionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			updateRegs();
			dataLS.setSelectedIndex(PC);
			currentSim.repaint();			
		}
	};

	Timer timer;

	Simulator() {

		emptyMemory();
		secs = 3;

		JPanel panel = new JPanel(new BorderLayout());
		setContentPane(panel);

		JPanel regsPanel = new JPanel(new GridBagLayout());

		JLabel irL = null, pcL = null, marL = null, mdrL = null, acL = null;

		String labelsS[] = {"IR", "PC", "AC", "MAR", "MDR"};
		JLabel labelsL[] = {irL, pcL, acL, marL, mdrL};
		JTextField txtfdsTF[] = {irTF, pcTF, acTF, marTF, mdrTF};


		dataLS = new JList<String>(data);
		JScrollPane dataSP = new JScrollPane(dataLS);
		JPanel leftPanel = new JPanel(new GridBagLayout());
		leftPanel.add(dataSP, Tools.makeC(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH));

		int x = 1;
		for(int i=0; i < labelsS.length; i++) {
			labelsL[i] = new JLabel( labelsS[i] );
			labelsL[i].setHorizontalAlignment(JLabel.CENTER);
			txtfdsTF[i] = new JTextField("");
			txtfdsTF[i].setColumns(8);

			regsPanel.add(labelsL[i], Tools.makeC(x, 1, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
			txtfdsTF[i].setEditable(false);
			txtfdsTF[i].setHorizontalAlignment(JTextField.CENTER);
			txtfdsTF[i].setBackground(Color.WHITE);
			txtfdsTF[i].setFont(Tools.makeFont(Font.SANS_SERIF, "BOLD", 12));
			regsPanel.add(txtfdsTF[i], Tools.makeC(x, 2, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));

			x += 2;
		}

		irTF = txtfdsTF[0];
		pcTF = txtfdsTF[1];
		acTF = txtfdsTF[2];
		marTF = txtfdsTF[3];
		mdrTF = txtfdsTF[4];

		JPanel rightPanel = new JPanel(new GridBagLayout());

		regsPanel.setBorder(new TitledBorder("Registros"));
		rightPanel.add(regsPanel, Tools.makeC(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));

		JPanel editPanel = new JPanel(new GridBagLayout());
		addressSN = new JSpinner(new SpinnerNumberModel(0, 0, MEMSIZE - 1, 1));
		addressSN.setEditor(new JSpinner.NumberEditor(addressSN, "000"));
		editPanel.add(addressSN, Tools.makeC(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
		contentTF = new JTextField(11);
		contentTF.addActionListener(new loadButtonHandler());
		editPanel.add(contentTF, Tools.makeC(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
		typeCB = new JComboBox<String>( new String[]{"Ensamblador", "Valor"} );
		editPanel.add(typeCB, Tools.makeC(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
		JButton loadB = new JButton("Cargar");
		loadB.addActionListener(new loadButtonHandler());
		editPanel.add(loadB, Tools.makeC(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
		JButton loadFileB = new JButton("Cargar desde archivo");
		loadFileB.addActionListener(new loadFileButtonHandler());
		editPanel.add(loadFileB, Tools.makeC(1, 2));
		editPanel.setBorder(new TitledBorder("Editar memoria"));
		rightPanel.add(editPanel, Tools.makeC(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));

		JPanel optionsPanel = new JPanel(new GridBagLayout());
		optionsPanel.setBorder(new TitledBorder("Configuración"));
		JButton emptyB = new JButton("Vaciar memoria");
		emptyB.addActionListener(new emptyButtonHandler());
		optionsPanel.add(emptyB, Tools.makeC(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
		intervalSL = new JSlider(0, 10);
		intervalSL.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(), "Intervalo en segundos entre ejecución de microoperaciones", TitledBorder.CENTER, TitledBorder.CENTER));
		intervalSL.setPaintLabels(true);
		intervalSL.setPaintTicks(true);
		intervalSL.setMinorTickSpacing(1);
		intervalSL.setMajorTickSpacing(1);
		intervalSL.setValue(secs);
		intervalSL.addChangeListener(new intervalSliderHandler());
		optionsPanel.add(intervalSL, Tools.makeC(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));
		rightPanel.add(optionsPanel, Tools.makeC(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));

		executeB = new JButton("Ejecutar");
		executeB.addActionListener(new executeButtonHandler());
		rightPanel.add(executeB, Tools.makeC(0, 3));
		JLabel infoL = new JLabel("Mariano García Alipi, Humberto González Sánchez, Rodrigo Bilbao Arrieta");
		infoL.setFont(Tools.makeFont(Font.SANS_SERIF, "ITALICS", 11));
		infoL.setHorizontalAlignment(JLabel.CENTER);
		rightPanel.add(infoL, Tools.makeC(0, -1, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL));

		JSplitPane splitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		panel.add(splitPanel);
		splitPanel.setDividerLocation(250);
		splitPanel.setEnabled(false);

		updateRegs();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800, 600);
		setMinimumSize(new Dimension(800, 600));
		setMaximumSize(new Dimension(800, 600));
		setPreferredSize(new Dimension(800, 600));
		setResizable(false);
		setLocationRelativeTo(null);
		setTitle("Simulador");
		setVisible(true);
	}

	// Función que vacía la dirección de memoria.
	// Parámetros: int con la dirección.
	// Valor de retorno: ninguno.
	void clearAt(int pos) {
		data[pos] = fillTo(pos, 3);
	}

	// Función que guarda en la dirección de memoria el string.
	// Parámetros: int con la posición, string con el contenido.
	// Valor de retorno: ninguno.
	void saveTo(int pos, String cont) {
		// If it already contains something...
		if(data[pos].length() != 3)
			// Empty and add cell number.
			clearAt(pos);
		// Add content.
		data[pos] = fillTo(pos, 3) + SPC + cont;
	}

	// Función que regresa el contenido de una dirección de memoria, removiendo el índice.
	// Parámetros: int con la posición.
	// Valor de retorno: string con el contenido.
	String readFrom(int pos) {
		String result = data[pos].substring(3);
		if(!result.isEmpty()) {
			// Remove blank spaces that separated memory address and content.
			result = result.substring(SPC.length());
		}
		return result;
	}

	// Función que rellena con ceros el número indicado de posiciones.
	// Parámetros: int con el número a mostrar, int con la cantidad de posiciones.
	// Valor de retorno: string con el número rellenado.
	String fillTo(int num, int pos) {
		return ("00000000000000").substring(0, pos - ("" + num).length()) + num;
	}

	// Función que obtiene el código de operación según un string.
	// Parámetro: el string con la operación (por ejemplo: "LDA").
	// Valor de retorno: int del código de operación (por ejemplo: 2).
	int getOpCode(String operation) {
		for(int i=0; i<9; i++) {
			if(codes[i].equals(operation))
				return i;
		}
		return -1;
	}

	// Función que obtiene el tipo de direccionamiento según un string.
	// Parámetro: el string con una letra que representa el tipo (por ejemplo: "ABS").
	// Valor de retorno: int del tipo de direccionamiento (por ejemplo: 1).
	int getAddrType(String input) {
		if(input.equals("ABS"))
			return 1;
		if(input.equals("IND"))
			return 2;
		if(input.equals("INM"))
			return 3;
		if(input.equals("REL"))
			return 4;
		return -1;
	}

	// Función que vacía la memoria del simulador.
	// Parámetros: ninguno.
	// Valor de retorno: ninguno.
	void emptyMemory() {
		for(int i = 0; i < MEMSIZE; i++) {
			data[i] = fillTo(i, 3);
		}
		PC = PCprev = 0;
		MAR = MDR = AC = IR = "000000";
	}

	/*
	Funcion que completa el PC con los ceros requeridos para mostrarlo.
  	Parametros: PC.
	Valor de retorno: string con el PC completo.
	 */
	String completePC(int iPC) {
		String myPC = "" + iPC;
		String complete = "";

		for(int i = 0; i < 3 - myPC.length(); i++) {
			complete += "0";
		}
		complete += myPC;
		return complete;
	}

	// Función que convierte de maquinal a ensamblador.
	// Parámetros: string con la instrucción en maquinal.
	// Valor de retorno: string con la instrucción en esamblador.
	String convertAssemb(String inst) {
		String opCode = inst.substring(0,2), code;
		char addrType = inst.charAt(2);
		String addr = "";
		String parameter = inst.substring(3);

		code = codes[parsear(opCode)];

		switch(addrType) {
		case '1':
			addr = "ABS";
			break;
		case '2':
			addr = "IND";
			break;
		case '3':
			addr = "INM";
			break;
		case '4':
			addr = "REL";
			break;
		}

		if(code.equals("NOP") || code.equals("CLA") || code.equals("NEG") || code.equals("HLT"))
			parameter = "";

		return code + " " + addr + " " + parameter;
	}

	/*
	Funcion que muestra en pantalla los registros y sus cambios
  	Parametros: ninguno.
  	valor de retorno: ninguno.
//	 */
	void displayChanges() {

		updateRegs();

		dataLS.setSelectedIndex(PC);
		currentSim.repaint();

		//		timer = new Timer(secs * 1000, actionListener);
		//		timer.start();

		//		try {
		//			Thread.sleep(secs * 1000);
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
	}

	// Función que regresa un string que contiene cómo se mostrará la opción de acuerdo con su estado (activado/desactivado).
	// Parámetros: una variable booleana.
	// Valor de retorno: un string que contiene cómo se mostrará la opción.
	String getBoolX(boolean var) {
		if(var)
			return "[X]";
		else
			return "[ ]";
	}

	int parsear(String str) {
		if(str.isEmpty())
			return 0;
		if(Double.isNaN(Double.parseDouble(str))) {
			return 0;
		} else {
			return Integer.parseInt(str);
		}
	}


	void showError(String message, String title) {
		JOptionPane.showMessageDialog(currentSim, message, title, JOptionPane.ERROR_MESSAGE);
	}

	String compile(String line) {
		// ints to store operation code, addressing type and parameter value. They will be merged to form an instruction.
		int opCode, addrType;
		String segment, param;
		String ERROR = "ERROR";

		// If the line is empty, store as empty string("").
		if(line.isEmpty())
			return "";

		if( (line.charAt(0) == '+' || line.charAt(0) == '-') && line.length() == 6) {
			return line;
		} else {
			Scanner input = new Scanner(line);
			String val = "";

			// This should contain the operation (e.g. "LDA" or "CLA").
			segment = input.next();
			opCode = getOpCode(segment);

			if(opCode == -1) {
				showError("Dato no válido.", "Error");
				input.close();
				return ERROR;
			}

			// If operation is an instruction which doesn't take parameters...
			if(codes[opCode] == "HLT" || codes[opCode] == "NEG" || codes[opCode] == "CLA" || codes[opCode] == "NOP") {
				val += fillTo(opCode, 2) + "0000";
				input.close();
				return val;
				// If operation code is valid...
			} else if(opCode != -1) {

				// This should contain the addressing type (e.g. "ABS" or "INM").
				segment = input.next();
				addrType = getAddrType(segment);

				// If the addressing type is valid...
				if(addrType != -1) {

					// This should contain the parameter value, a three digit number (e.g. "020" or "123").
					segment = input.next();

					if(segment.length() == 3) {
						// If addressing type is ABS or IND...
						if(addrType == 1 || addrType == 2) {
							if(segment.charAt(0) == '+'  || segment.charAt(0) == '-') {
								showError("El parámetro no puede tener signo para ese tipo de direccionamiento (\"" + addrType + "\").", "Error");
								input.close();
								return ERROR;
							}
						}

						param = segment;

						val += fillTo(opCode, 2);
						val += addrType;
						val += param;

						input.close();
						return val;

					} else {
						showError("No se encontró un valor de parámetro válido.", "Error");
						input.close();
						return ERROR;
					}
				} else {
					showError("No se encontró un tipo de direccionamiento válido.", "Error");
					input.close();
					return ERROR;
				}

				// If operation code is invalid but it's a value (values start with the sign and must be six characters long)...
			} else if( (line.charAt(0) == '+' || line.charAt(0) == '-') &&  line.length() == 6 ) {
				input.close();
				return line;

				// If operation code is invalid and it's not a value...
			} else {
				showError("No se encontró una operación o valor/dato válido.", "Error");
				input.close();
				return ERROR;
			}
		}
	}

	/*
	  Función que edita el contenido de una dirección de memoria directamente.
	  Parámetros: ninguno.
	  Valor de retorno: ninguno.
	 */
	void editMemoryDirectly(int dir, String val) {

		// Validation process starts here.

		if(val.isEmpty())
			clearAt(dir);

		// If it's data/value...
		if(val.charAt(0) == '+' || val.charAt(0) == '-') {
			if(val.length() == 6)
				saveTo(dir, val);
			else {
				showError("El valor debe ser de seis caracteres.", "Error");
				return;
			}
		} else {
			// If it's an instruction...
			String opCode, param;
			int iOpCode, iAddr;

			opCode = val.substring(0, 2);
			iOpCode = parsear(opCode);
			// If it's a valid operation code...
			if(iOpCode >= 0 && iOpCode < 9) {
				iAddr = parsear(val.substring(2, 3));

				// If it's an instruction which doesn't take parameters, set addressing type to 1 to make it valid.
				if(iOpCode == 0 || iOpCode == 1 || iOpCode == 6 || iOpCode == 8)
					iAddr = 1;

				// If it's a valid addressing type...
				if(iAddr >= 1 && iAddr <= 4) {
					param = val.substring(3);

					// If parameter is three characters long...
					if(param.length() == 3) {

						// If addressing type is ABS or IND...
						if(iAddr == 1 || iAddr == 2) {
							if(param.charAt(0) == '+'  || param.charAt(0) == '-') {
								showError("El parámetro no puede tener signo para ese tipo de direccionamiento (\"" + iAddr + "\").", "Error");
								return;
							}
						}

						// Success. Save to memory.
						saveTo(dir, val);
						//	  				cout << "Dirección de memoria modificada exitosamente.";
					} else {
						showError("El parámetro debe ser de tres caracteres.", "Error");
						return;
					}
				} else {
					showError("El tipo de direccionamiento \"" + iAddr + "\" no es válido.", "Error");;
					return;
				}
			} else {
				showError("El código de operación \"" + iOpCode + "\" no es válido.", "Error");
				return;
			}

		}
		contentTF.setText("");
		addressSN.setValue(Integer.parseInt(addressSN.getValue().toString()) + 1);
	}

	/*
	  Función que edita el contenido de una dirección de memoria con la instrucción dada.
	  Parámetros: ninguno.
	  Valor de retorno: ninguno.
	 */
	void editMemory(int dir, String line) {
		line = line.toUpperCase();

		// "Compiling" process starts here.
		String val = compile(line); 

		if(val.isEmpty())
			clearAt(dir);
		else if(!val.equals("ERROR")) {
			saveTo(dir, val);
			contentTF.setText("");
			addressSN.setValue(Integer.parseInt(addressSN.getValue().toString()) + 1);
		}
	}

	// Función que completa una palabra al número necesario de caracteres, rellenando con 0.
	// Parámetros: un número entero.
	// Valor de retorno: string con el AC ajustado.
	String completeAC(int iTemp) {

		String str = "" + (iTemp);
		String complete = "";

		if(str.charAt(0) == '-') {
			str = str.substring(1);
			complete += '-';
		} else {
			complete += '+';
		}

		for(int i = 0; i < 5 - str.length(); i++) {
			complete += 0;
		}

		complete += str;

		return complete;
	}

	/*
	  Funcion que realiza la operacion CLA y pone en 0 el acumulador.
	  Parametros: Ninguno.
	  Valor de retorno: Ninguno.
	 */
	void opCLA() {
		AC = "+00000";
		displayChanges();
	}

	/*
	  Funcion que realiza la operacion LDA.
	  de memoria.
	  Parametros: el tipo de direccionamiento y el parametro de la instruccion ([IR]2-0).
	  Valor de retorno: ninguno.
	 */
	void opLDA(String sDireccionamiento, String sExtra) {
		int iDir, iTemp;
		String sContenido;

		switch (sDireccionamiento.charAt(0)) {
		// Absoluto
		case '1': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir);
			displayChanges();
			AC = MDR;
			displayChanges();
			break;
		}
		// Indirecto
		case '2': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir);
			displayChanges();
			MAR = MDR;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir);
			displayChanges();
			AC = MDR;
			displayChanges();
			break;
		}
		// Inmediato
		case '3': {
			sContenido = completeAC(parsear(sExtra)); // Completa el string con 0 y signo respectivamente
			AC = sContenido;
			displayChanges();
			break;
		}
		// Relativo
		case '4': {
			iTemp = parsear(sExtra);
			if (PC + iTemp < 0 || PC + iTemp > 999) {
				showError("OUT OF BOUNDS", "Error");
			}
			else {
				if (PC + iTemp < 100) {
					if (PC + iTemp < 10) {
						MAR = "00" + (PC + iTemp);
					}
					else {
						MAR = "0" + (PC + iTemp);
					}
				}
				else {
					MAR = "" + (PC + iTemp);
				}
				displayChanges();
				MDR = readFrom(PC + iTemp); // MMRead
				displayChanges();
				AC = MDR;
				displayChanges();
			}
			break;
		}
		default: {
			showError("INSTRUCCIÓN NO VÁLIDA", "Error");
		}
		}
	}

	/*
	  Funcion que realiza la operacion STA.
	  Parametros: el tipo de direccionamiento y el parametro de la instruccion ([IR]2-0).
	  Valor de retorno: ninguno.
	 */
	void opSTA(String sDireccionamiento, String sExtra) {
		int iDir, iTemp;

		switch (sDireccionamiento.charAt(0)) {
		// Absoluto
		case '1': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = AC;
			displayChanges();
			//			saveTo(iDir, MDR); // MMWrite
			saveTo(iDir, MDR);
			displayChanges();
			break;
		}
		// Indirecto
		case '2': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			//			MDR = readFrom(iDir);
			MDR = readFrom(iDir);
			displayChanges();
			MAR = MDR;
			displayChanges();
			iDir = parsear(MAR);
			MDR = AC;
			displayChanges();
			saveTo(iDir, MDR); // MMWrite
			displayChanges();
			break;
		}
		// Relativo
		case '4': {
			iTemp = parsear(sExtra);
			if (PC + iTemp < 0 || PC + iTemp > 999) {
				JOptionPane.showConfirmDialog(currentSim, "OUT OF BOUNDS", "Error", JOptionPane.ERROR_MESSAGE);
			}
			else {
				if (PC + iTemp < 100) {
					if (PC + iTemp < 10) {
						MAR = "00" + (PC + iTemp);
					}
					else {
						MAR = "0" + (PC + iTemp);
					}
				}
				else {
					MAR = "" + (PC + iTemp);
				}
				displayChanges();
				MDR = AC;
				displayChanges();
				saveTo(PC + iTemp, MDR); // MMWrite
				displayChanges();
			}
			break;
		}
		default: {
			showError("INSTRUCCIÓN NO VÁLIDA", "Error");
		}
		}
	}

	/*
	  Funcion que realiza la operacion ADD.
	  Parametros: el tipo de direccionamiento y el parametro de la instruccion ([IR]2-0).
	  Valor de retorno: ninguno.
	 */
	void opADD(String sDireccionamiento, String sExtra) {
		int iDir, iTemp, iTemp2;

		switch (sDireccionamiento.charAt(0)) {
		// Absoluto
		case '1': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			//			MDR = readFrom(iDir); // MMRead
			MDR = readFrom(iDir);
			displayChanges();
			iTemp = parsear(MDR);
			iTemp2 = parsear(AC);
			if (iTemp + iTemp2 > 99999 || iTemp + iTemp2 < -99999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				AC = completeAC(iTemp + iTemp2);
				displayChanges();
			}
			break;
		}
		// Indirecto
		case '2': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir); // MMRead
			displayChanges();
			MAR = MDR;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir); // MMRead
			displayChanges();
			iTemp = parsear(MDR);
			iTemp2 = parsear(AC);
			if (iTemp + iTemp2 > 99999 || iTemp + iTemp2 < -99999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				AC = completeAC(iTemp + iTemp2);
				displayChanges();
			}
			break;
		}
		// Inmediato
		case '3': {
			iTemp = parsear(sExtra);
			iTemp2 = parsear(AC);
			if (iTemp + iTemp2 > 99999 || iTemp + iTemp2 < -99999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				AC = completeAC(iTemp + iTemp2);
				displayChanges();
			}
			break;
		}
		// Relativo
		case '4': {
			iTemp = parsear(sExtra);
			if (PC + iTemp < 0 || PC + iTemp > 999) {
				JOptionPane.showConfirmDialog(currentSim, "OUT OF BOUNDS", "Error", JOptionPane.ERROR_MESSAGE);
			}
			else {
				if (PC + iTemp < 100) {
					if (PC + iTemp < 10) {
						MAR = "00" + "" + (PC + iTemp);
					}
					else {
						MAR = "0" + "" + (PC + iTemp);
					}
				}
				else {
					MAR = "" + (PC + iTemp);
				}
				displayChanges();
				MDR = readFrom(PC + iTemp); // MMRead
				displayChanges();
				iTemp = parsear(MDR);
				iTemp2 = parsear(AC);
				AC = completeAC(iTemp + iTemp2);
				displayChanges();
			}
			break;
		}
		default: {
			showError("INSTRUCCIÓN NO VÁLIDA", "Error");
		}
		}
	}

	/*
	  Funcion que realiza la operacion SUB.
	  Parametros: el tipo de direccionamiento y el parametro de la instruccion ([IR]2-0).
	  Valor de retorno: ninguno.
	 */
	void opSUB(String sDireccionamiento, String sExtra) {
		int iDir, iTemp, iTemp2;

		switch (sDireccionamiento.charAt(0)) {
		// Absoluto
		case '1': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir); // MMRead
			displayChanges();
			iTemp = parsear(MDR);
			iTemp2 = parsear(AC);
			if (iTemp2 - iTemp > 99999 || iTemp2 - iTemp < -99999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				AC = completeAC(iTemp2 - iTemp);
				displayChanges();
			}
			break;
		}
		// Indirecto
		case '2': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir); // MMRead
			displayChanges();
			MAR = MDR;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir); // MMRead
			displayChanges();
			iTemp = parsear(MDR);
			iTemp2 = parsear(AC);
			if (iTemp2 - iTemp > 99999 || iTemp2 - iTemp < -99999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				AC = completeAC(iTemp2 - iTemp);
				displayChanges();
			}
			break;
		}
		// Inmediato
		case '3': {
			iTemp = parsear(sExtra);
			iTemp2 = parsear(AC);
			if (iTemp2 - iTemp > 99999 || iTemp2 - iTemp < -99999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				AC = completeAC(iTemp2 - iTemp);
				displayChanges();
			}
			break;
		}
		// Relativo
		case '4': {
			iTemp = parsear(sExtra);
			if (PC + iTemp < 0 || PC + iTemp > 999) {
				JOptionPane.showConfirmDialog(currentSim, "OVERFLOW", "Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
			}
			else {
				if (PC + iTemp < 100) {
					if (PC + iTemp < 10) {
						MAR = "00" + "" + (PC + iTemp);
					}
					else {
						MAR = "0" + "" + (PC + iTemp);
					}
				}
				else {
					MAR = "" + (PC + iTemp);
				}
				displayChanges();
				MDR = readFrom(PC + iTemp); // MMRead
				displayChanges();
				iTemp = parsear(MDR);
				iTemp2 = parsear(AC);
				AC = completeAC(iTemp2 - iTemp);
				displayChanges();
			}
			break;
		}
		default: {
			showError("INSTRUCCIÓN NO VÁLIDA", "Error");
		}
		}
	}

	/*
	  Funcion que realiza la operacion NEG.
	  Parametros: ninguno.
	  Valor de retorno: ninguno.
	 */
	void opNEG() {
		int iTemp;
		iTemp = parsear(AC);
		iTemp *= -1;
		AC = completeAC(iTemp);
		displayChanges();
	}

	/*
	  Funcion que realiza la operacion JMP.
	  Parametros: ninguno.
	  Valor de retorno: ninguno.
	 */
	void opJMP(String sDireccionamiento, String sExtra) {
		int iDir, iTemp;

		switch (sDireccionamiento.charAt(0)) {
		// Absoluto
		case '1': {
			PCprev = PC;
			PC = parsear(sExtra);
			displayChanges();
			break;
		}
		// Indirecto
		case '2': {
			MAR = sExtra;
			displayChanges();
			iDir = parsear(MAR);
			MDR = readFrom(iDir); // MMRead
			displayChanges();
			MAR = MDR;
			displayChanges();
			displayChanges();
			PCprev = PC;
			PC = parsear(MAR);
			displayChanges();
			break;
		}
		// Relativo
		case '4': {
			iTemp = parsear(sExtra);
			if (PC + iTemp < 0 || PC + iTemp > 999) {
				JOptionPane.showConfirmDialog(currentSim, "OUT OF BOUNDS", "Error", JOptionPane.ERROR_MESSAGE);
			}
			else {
				if (PC + iTemp < 100) {
					if (PC + iTemp < 10) {
						MAR = "00" + "" + (PC + iTemp);
					}
					else {
						MAR = "0" + "" + (PC + iTemp);
					}
				}
				else {
					MAR = "" + (PC + iTemp);
				}
				displayChanges();
				PCprev = PC;
				PC = parsear(MAR);
				displayChanges();
			}
			break;
		}
		default: {
			showError("INPUT ERROR", "Error");
			bContinue = false;
		}
		}
	}

	void updateRegs() {
		pcTF.setText("" + fillTo(PC, 3));
		irTF.setText(IR);
		marTF.setText(MAR);
		mdrTF.setText(MDR);
		acTF.setText(AC);
	}

	/*
	  Funcion que ejecuta las instrucciones que se encuentren en la memoria
	  Parámetros: ninguno.
	  Valor de retorno: ninguno.
	 */
	void execute() {
		String sOpCode, sAdType, sExtra;
		bContinue = true;
		PC = 0;
		PCprev = 0;
		displayChanges();


		while (PC < 1000 && bContinue) {

			// Remove memory address.
			IR = data[PC].substring(3);

			if(!IR.isEmpty()) {

				// Remove blank spaces that separated memory address and content.
				IR = IR.substring(SPC.length());

				if (!IR.isEmpty() && IR.charAt(0) != '+' && IR.charAt(0) != '-') {
					sOpCode = IR.substring(0,2);
					sAdType = IR.substring(2, 3);
					sExtra = IR.substring(3);

					if (!sOpCode.equals("07")) {
						PCprev = PC++;
					}

					// NOP
					if (sOpCode.equals("00")) {
						// JEJE SOY UN NOP e.e
					}
					// CLA
					else if (sOpCode.equals("01")) {
						opCLA();
					}
					// LDA
					else if (sOpCode.equals("02")) {
						opLDA(sAdType, sExtra);
					}
					// STA
					else if (sOpCode.equals("03")) {
						opSTA(sAdType, sExtra);
					}
					// ADD
					else if (sOpCode.equals("04")) {
						opADD(sAdType, sExtra);
					}
					// SUB
					else if (sOpCode.equals("05")) {
						opSUB(sAdType, sExtra);
					}
					// NEG
					else if (sOpCode.equals("06")) {
						opNEG();
					}
					// JMP
					else if (sOpCode.equals("07")) {
						opJMP(sAdType, sExtra);
					}
					// HLT
					else if (sOpCode.equals("08")) {
						bContinue = false;
						displayChanges();
					}
				}
				else {
					PCprev = PC++;
				}
			} else {
				PCprev = PC++;
			}
		} 
	}

	public static void main(String[] args) {
		currentSim = new Simulator();
	}

	class loadButtonHandler implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			int address = parsear(addressSN.getValue().toString());
			String content = contentTF.getText();
			if(typeCB.getSelectedItem().toString() == "Valor") {
				editMemoryDirectly(address, content);
			} else if(typeCB.getSelectedItem().toString() == "Ensamblador") {
				editMemory(address, content);
			}
			dataLS.setSelectedIndex(address);
			dataLS.ensureIndexIsVisible(address);

			currentSim.repaint();
		}
	} 

	class loadFileButtonHandler implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			File file;
			JFileChooser chooserFC = new JFileChooser();
			chooserFC.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooserFC.setDialogTitle("Seleccione el archivo");
			chooserFC.setAcceptAllFileFilterUsed(false);

			int returnVal = chooserFC.showOpenDialog(currentSim);
			file = chooserFC.getSelectedFile();

			Scanner input = null;

			if(returnVal == JFileChooser.CANCEL_OPTION)
				return;

			try {
				input = new Scanner(file);

				String line, compiled;
				int i = 0;
				while(input.hasNextLine()) {
					line = input.nextLine();
					compiled = compile(line);
					if(!compiled.equals("ERROR")) {
						saveTo(i, compiled);
						i++;
					} else {
						showError("El error anterior está en la línea " + fillTo(i + 1, 3) + ".", "Error");
					}
				}
				input.close();		
				currentSim.repaint();
				JOptionPane.showMessageDialog(currentSim, "Carga de archivo finalizada.", "Carga finalizada", JOptionPane.INFORMATION_MESSAGE);

			} catch (Exception ex) {
				showError("No se pudo abrir el archivo.", "Error");
			}
		}
	} 

	class emptyButtonHandler implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(JOptionPane.showConfirmDialog(currentSim, "¿Está seguro de que desea vaciar la memoria?", "Advertencia", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
				emptyMemory();
			updateRegs();
			currentSim.repaint();
		}
	}

	class executeButtonHandler implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			executeB.setEnabled(false);
			execute();
			executeB.setEnabled(true);

		}
	}

	class intervalSliderHandler implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			secs = intervalSL.getValue();

		}

	}

}