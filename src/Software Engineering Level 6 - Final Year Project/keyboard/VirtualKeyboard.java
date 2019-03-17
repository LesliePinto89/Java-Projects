package keyboard;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.imageio.ImageIO;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import keyboard.FeatureTabs;
import midi.Genre;
import tools.MIDIFileManager;
import tools.MIDIFilePlayer;
import tools.ProgramMainGUI;
import tools.ScreenPrompt;
import tools.SwingComponents;

/**
 * This class displays the piano software to the user, and acts as a base to the
 * class that constructs MIDI track files.
 * 
 */
public class VirtualKeyboard {

	private JSlider slider;
	private JButton homeButton;
	private ArrayList<JButton> naturalKeys = new ArrayList<JButton>();
	private static volatile VirtualKeyboard instance = null;

	private VirtualKeyboard() {
	}

	public static VirtualKeyboard getInstance() {
		if (instance == null) {
			synchronized (VirtualKeyboard.class) {
				if (instance == null) {
					instance = new VirtualKeyboard();
				}
			}
		}
		return instance;
	}

	
	private int PITCH_OCTAVE = 2;
	private int screenWidth = (int) SwingComponents.getInstance().getScreenWidth();
	private int screenHeight = (int) SwingComponents.getInstance().getScreenHeight();

	private int START_WHOTE_NOTE_POSITION = screenWidth / 125;
	private int START_SHARP_NOTE_POSITION = screenWidth / 45;

	// This panel will later be updated, utilised with the validate and repaint
	// functions
	private JPanel originalScreenPrompt = null;
	private JPanel contentPane = new JPanel();

	// needs to be a layered pane to keep sizing
	private JLayeredPane allPianoKeysLayeredPanel = new JLayeredPane();
	private JPanel panelHoldPianoKeys = new JPanel();
	private JPanel pianoBackingPanel = new JPanel();
	private JFrame freeGuiFrame;
	private JFrame learnGuiFrame;
	private JPanel controlPanel = new JPanel();
	private JPanel redPanelkeysHolder = new JPanel();
	private JPanel buttonHolder = new JPanel(new GridBagLayout());
	private SwingComponents components = SwingComponents.getInstance();

	private JPanel innerButtonHolder = new JPanel(new GridBagLayout());

	private GridBagConstraints aConstraint;
	private boolean userChoice;

	// TEST
	//private JPanel pane = new JPanel(new GridBagLayout());
	private GridBagConstraints c = new GridBagConstraints();

	////////////////////////
	public void storeButtons(JButton noteButton) {
		naturalKeys.add(noteButton);
	}

	public ArrayList<JButton> getButtons() {
		return naturalKeys;
	}

	/**
	 * Style whole and accidental keys colour
	 * 
	 * @param button
	 *            - JButton piano key to style
	 */
	public void styleKeys(JButton button) {
		if (button.getText().contains("#")) {
			button.setForeground(Color.WHITE);
			button.setBackground(Color.BLACK);
			allPianoKeysLayeredPanel.add(button, new Integer(1));
		} else {
			button.setBackground(Color.WHITE);
			allPianoKeysLayeredPanel.add(button, new Integer(0));
		}
	}

	/**
	 * Creates the piano's accidental (black) keys.
	 * 
	 */
	public void createSharpKeys() throws InvalidMidiDataException, MidiUnavailableException {
		PITCH_OCTAVE = 1;
		EnumSet<Note.SharpNoteType> sharpKeysEnums = EnumSet.allOf(Note.SharpNoteType.class);
		while (PITCH_OCTAVE < 6) {
			for (Note.SharpNoteType getNote : sharpKeysEnums) {
				if (getNote.getSharp().equals("C#")) {
					PITCH_OCTAVE++;

				}
				JButton button = new JButton(getNote.getSharp() + Integer.toString(PITCH_OCTAVE));
				if (button.getText().startsWith("C") && PITCH_OCTAVE >= 3) {
					//START_SHARP_NOTE_POSITION += 25;
					START_SHARP_NOTE_POSITION += screenWidth / 55;
				}

				else if (button.getText().startsWith("F")) {
					//START_SHARP_NOTE_POSITION += 30;
					START_SHARP_NOTE_POSITION += screenWidth / 42;
					
					
					//+= screenWidth / 38;
				}
				// button.setBounds(START_SHARP_NOTE_POSITION, 145, 20, 126);
				button.setBounds(START_SHARP_NOTE_POSITION, 145, screenWidth / 55, 126);

				button.setFont(new Font("Arial", Font.PLAIN, 6));
				button.setVerticalAlignment(SwingConstants.BOTTOM);
				button.setMargin(new Insets(1, 1, 1, 1));

				styleKeys(button);
				storeButtons(button);

				// START_SHARP_NOTE_POSITION += 37;
				START_SHARP_NOTE_POSITION += screenWidth / 35.5;
			}
		}
	}

	/**
	 * Creates the piano's whole (white) keys.
	 * 
	 */
	public void createWholeKeys() throws InvalidMidiDataException, MidiUnavailableException {

		EnumSet<Note.NoteType> naturalKeysEnums = EnumSet.allOf(Note.NoteType.class);
		boolean endOctave = false;
		while (endOctave == false) {
			for (Note.NoteType getNote : naturalKeysEnums) {
				JButton button = new JButton(getNote.toString() + Integer.toString(PITCH_OCTAVE));
				button.setBounds(START_WHOTE_NOTE_POSITION, 145, screenWidth / 37, 196);
				button.setFont(new Font("Arial", Font.PLAIN, 10));
				button.setVerticalAlignment(SwingConstants.BOTTOM);
				button.setMargin(new Insets(1, 1, 1, 1));

				// Fixed octave number matches 61 notes keyboard
				if (getNote.toString().equals("C") && PITCH_OCTAVE == 7) {
					styleKeys(button);
					storeButtons(button);
					endOctave = true;
					break;
				}
				styleKeys(button);
				storeButtons(button);

				START_WHOTE_NOTE_POSITION += screenWidth / 38;
				if (getNote.toString().equals("B")) {
					PITCH_OCTAVE++;
				}
			}
		}
	}

	/**
	 * Adjust all notes volume based on slider value.
	 */
	public void volumeToggle() {
		slider = new JSlider(0, 100, 71);
		slider.setPreferredSize(new Dimension(150, 50));
		slider.setMinimumSize(new Dimension(150, 50));
		slider.setBackground(Color.decode("#404040"));
		slider.setBorder(new LineBorder(Color.decode("#303030")));
		KeyboardInteractions volumeSliderListener = new KeyboardInteractions(slider);
		slider.addChangeListener(volumeSliderListener);

		aConstraint = components.conditionalConstraints(1, 1, 0, 0,	GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.LINE_START;	
		innerButtonHolder.add(slider, aConstraint);
	}
	
	public void createSongButton() throws IOException {
		
		BufferedImage home = ImageIO.read(new File("src/Images/Create Song.png"));
		homeButton = components.customJButtonTwo(70, 50, "Home",home,
				Color.decode("#404040"),Color.decode("#303030"),false );
		
		ActionListener genreButtonActionListener = new KeyboardInteractions(homeButton);
		homeButton.addActionListener(genreButtonActionListener);
		
		aConstraint = components.conditionalConstraints(1, 1, 1, 0,	GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.LINE_START;
		aConstraint.gridwidth =1;
		innerButtonHolder.add(homeButton, aConstraint);
		
	}

	public void debugModeButton() throws InvalidMidiDataException, IOException {
		BufferedImage playOff = ImageIO.read(new File("src/Images/DebugImage.png"));
	    JToggleButton debugMIDI =components.featureJToggleButton(false,50, 42, "Debug","Debug", Color.decode("#404040"),
		Color.WHITE,Color.decode("#303030"), false,playOff,null,false);
		ActionListener debugButtonActionListener = new KeyboardInteractions(debugMIDI);
		debugMIDI.addActionListener(debugButtonActionListener);
		
		aConstraint = components.conditionalConstraints(1, 1, 2, 0,	GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.LINE_START;
		aConstraint.gridwidth =1;
		
		innerButtonHolder.add(debugMIDI, aConstraint);
	}

	public void recordButton() throws InvalidMidiDataException, IOException {	
		BufferedImage recOff = ImageIO.read(new File("src/Images/recordOff.png"));
		BufferedImage recOn = ImageIO.read(new File("src/Images/recordOn.png"));
		
		JToggleButton recordMIDI =components.featureJToggleButton(true,70, 42, "Off","recordButton", 
				Color.decode("#404040"),Color.WHITE,Color.decode("#303030"), false,recOff,recOn,false);

		ActionListener recordButtonActionListener = new KeyboardInteractions(recordMIDI);
		recordMIDI.addActionListener(recordButtonActionListener);
		
		aConstraint = components.conditionalConstraints(1, 1, 3, 0,GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.LINE_START;
		aConstraint.gridwidth =1;
		
		innerButtonHolder.add(recordMIDI, aConstraint);
	}

	public void playButton() throws IOException {
		BufferedImage playOff = ImageIO.read(new File("src/Images/play button off.png"));
		BufferedImage playOn = ImageIO.read(new File("src/Images/play button.png"));
		JToggleButton playMIDI =components.featureJToggleButtonAlt(50, 42,"playButton", 
				false,false,Color.decode("#404040"),Color.decode("#303030"),playOff,playOn);
		
		ActionListener playButtonActionListener = new KeyboardInteractions(playMIDI);
		playMIDI.addActionListener(playButtonActionListener);

		aConstraint = components.conditionalConstraints(1, 1, 4, 0,	GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.LINE_START;
		aConstraint.gridwidth =1;
		innerButtonHolder.add(playMIDI, aConstraint);
	}

	public void saveMIDIButton() throws IOException {
		
		BufferedImage saveOff = ImageIO.read(new File("src/Images/midi document.png"));
		BufferedImage saveOn = ImageIO.read(new File("src/Images/midi document - clicked.png"));
		JToggleButton saveMIDI =components.featureJToggleButtonAlt(50, 42,"saveButton", 
				false,false,Color.decode("#404040"),Color.decode("#303030"),saveOff,saveOn);
		
		ActionListener saveButtonActionListener = new KeyboardInteractions(saveMIDI);
		saveMIDI.addActionListener(saveButtonActionListener);
		aConstraint = components.conditionalConstraints(1, 1, 5, 0,GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.LINE_START;
		aConstraint.gridwidth =1;
		innerButtonHolder.add(saveMIDI, aConstraint);
	}

	public void freePlayOrMakeTrack() throws InvalidMidiDataException, MidiUnavailableException {
		ArrayList<JButton> retrievedList = getButtons();
		for (int i = 0; i < retrievedList.size(); i++) {
			JButton pressedNote = retrievedList.get(i);
			String noteName = pressedNote.getText();
			String noteOctave = noteName.substring(noteName.length() - 1, noteName.length());
			int octaveInNumber = Integer.parseInt(noteOctave);
			int getValue = Note.convertToPitch(noteName);

			// Store as notes in a map
			if (noteName.contains("#")) {
				Note aSharpNote = new Note(noteName, getValue, octaveInNumber, 100, "Sharp");
				aSharpNote.storeNotes(noteName, aSharpNote);
			} else {
				Note aNaturalNote = new Note(noteName, getValue, octaveInNumber, 100, "Natural");
				aNaturalNote.storeNotes(noteName, aNaturalNote);
			}

			MouseListener mouseListener = new KeyboardInteractions(pressedNote, getValue);
			pressedNote.addMouseListener(mouseListener);
		}
	}

	public void deleteFreeFrame(){
		freeGuiFrame = new JFrame();
	}
	
	public void deleteLearnFrame(){
		 learnGuiFrame = new JFrame();
	}
	
	public void reDrawFreeFrame(){
		freeGuiFrame.setVisible(true);
	}
	
	public void reDrawLearnFrame(){
		learnGuiFrame.setVisible(true);
	}

	/**
	 * Draws the main GUI layout, used in each Application GUI feature.
	 * 
	 * @throws IOException
	 * 
	 */
	public void drawKeyboardGUI(boolean mode) throws InvalidMidiDataException, MidiUnavailableException, IOException {

		
		contentPane.setLayout(new GridBagLayout());
		if (mode == false) {
			
			freeGuiFrame = components.customJFrame("", screenWidth, screenHeight, contentPane, null);
			contentPane.setBackground(Color.decode("#4169E1"));
			freeGuiFrame.setName("6100COMP: Computer Music Education Application - Free Play Mode");
		}

		else if (mode == true) {
			learnGuiFrame = components.customJFrame("", screenWidth, screenHeight, contentPane, null);
			contentPane.setBackground(Color.decode("#340D00"));
			learnGuiFrame.setName("6100COMP: Computer Music Education Application -Learn Mode");
		}
	}

	
	
	
//	public void home(){
//	
//			if(userChoice){
//				learnGuiFrame.setVisible(false);
//			}
//			else {
//			freeGuiFrame.setVisible(false);
//			}
//			ProgramMainGUI.getInstance().enableFrame();
//	}
	
	
	/**
	 * Main keyboard layout, with method calls to its various components.
	 * 
	 * @throws IOException
	 * @wbp.parser.entryPoint
	 * 
	 */
	public void createVirtualKeyboard(boolean mode)
			throws InvalidMidiDataException, MidiUnavailableException, IOException {
		userChoice = mode;

		drawKeyboardGUI(mode);
		
		if (mode) {
			addFeatureTabs();
			learnMode();
			debugModeButton();
		}
		
		else if (!mode) {
			aConstraint = components.conditionalConstraints(0, 1, 1, 0, GridBagConstraints.NONE);
			aConstraint.anchor = GridBagConstraints.PAGE_START;
			aConstraint.insets = new Insets (10,10,0,0);
			JPanel music = MIDIFilePlayer.getInstance().drawMusicPlayerGUI();
			contentPane.add(music, aConstraint);
		}
		drawPiano();
		volumeToggle();
		createSongButton();
		recordButton();
		playButton();
		saveMIDIButton();
	}

	public void learnMode() throws InvalidMidiDataException {
		addScreenPrompt();

	}

	public JPanel createSpeaker() throws IOException {

		JPanel speakerOne = new JPanel(new GridBagLayout());
		speakerOne.setBackground(Color.black);

		// speakerOne.setBorder(BorderFactory.createMatteBorder(2, 2, 2,
		// 2,Color.GRAY));
		speakerOne.setPreferredSize(new Dimension(300, 100));
		speakerOne.setMinimumSize(new Dimension(300, 100));

		// Chosen button holder color
		speakerOne.setBackground(Color.decode("#303030"));

		// Removing layout makes the inenr oane;s color show through
		JPanel holdSpeaker = new JPanel(new GridBagLayout());
		holdSpeaker.setPreferredSize(new Dimension(290, 90));
		holdSpeaker.setMinimumSize(new Dimension(290, 90));
		// holdSpeaker.setBackground(Color.BLUE);

		GridBagConstraints speakerConstraints = new GridBagConstraints();
		speakerConstraints.fill = GridBagConstraints.BOTH;
		speakerOne.add(holdSpeaker);

		BufferedImage image = ImageIO.read(new File("src/Images/Speaker Grill 4.jpg"));

		Image dimg = image.getScaledInstance(screenWidth / 4, screenHeight / 2, Image.SCALE_SMOOTH);
		JLabel picLabel = new JLabel(new ImageIcon(dimg));
		holdSpeaker.add(picLabel);

		return speakerOne;
	}

	public void drawPiano() throws IOException, InvalidMidiDataException {
		aConstraint = components.conditionalConstraints(1, 1, 0, 0, GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.PAGE_END;
		aConstraint.gridwidth = 2;

		// Black Panel
		if (freeGuiFrame !=null) {
		pianoBackingPanel = components.customPanelTwo(freeGuiFrame.getWidth() - 50, 322, Color.decode("#0D0707"),
				new GridBagLayout());}
		
		else if (learnGuiFrame !=null) {
			pianoBackingPanel = components.customPanelTwo(learnGuiFrame.getWidth() - 50, 322, Color.decode("#0D0707"),
					new GridBagLayout());
			}
		
		pianoBackingPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		contentPane.add(pianoBackingPanel, aConstraint);

		// Panel that holds all piano buttons
		aConstraint = components.conditionalConstraints(1, 1, 0, 0,	GridBagConstraints.NONE);
		aConstraint.anchor =GridBagConstraints.PAGE_START;
		controlPanel = components.customPanelTwo(screenWidth - 90, 122, Color.BLACK, new GridBagLayout());
		pianoBackingPanel.add(controlPanel, aConstraint);

		aConstraint = components.conditionalConstraints(1, 1, 0, 0, GridBagConstraints.VERTICAL);
		aConstraint.anchor =GridBagConstraints.CENTER;
		aConstraint.gridwidth = 5;
		aConstraint.gridheight = 0; // To carry size

		redPanelkeysHolder = components.customPanelTwo(screenWidth - 90, 122, Color.decode("#8C1400"), new BorderLayout());
		pianoBackingPanel.add(redPanelkeysHolder, aConstraint);
		
		
		redPanelkeysHolder.add(allPianoKeysLayeredPanel, BorderLayout.CENTER);

		aConstraint = components.conditionalConstraints(1, 1, 0, 0, GridBagConstraints.NONE);
		aConstraint.anchor = GridBagConstraints.LINE_START;
		
		
		JPanel speakerOne = createSpeaker();
		controlPanel.add(speakerOne, aConstraint);

		aConstraint = components.conditionalConstraints(1, 1, 1, 0,GridBagConstraints.HORIZONTAL);
		
		aConstraint.anchor = GridBagConstraints.LINE_START;
		
		aConstraint.insets = new Insets(0, 0, 0, 0);
		aConstraint.gridwidth = 1;
		int panelSize = (int) controlPanel.getPreferredSize().getHeight();
		buttonHolder.setBorder(BorderFactory.createMatteBorder(3, 3, 3, 3, Color.decode("#303030")));
		buttonHolder.setPreferredSize(new Dimension(screenWidth / 3 + 210, panelSize - 20));
		buttonHolder.setMinimumSize(new Dimension(screenWidth / 3 + 210, panelSize - 20));
		buttonHolder.setBackground(Color.decode("#383838"));
		controlPanel.add(buttonHolder, aConstraint);

		// Same GridBagConstraints used for bottom JPanel
		int innerPanelSize = (int) buttonHolder.getPreferredSize().getHeight();
		innerButtonHolder.setBorder(BorderFactory.createMatteBorder(3, 3, 3, 3, Color.decode("#303030")));
		innerButtonHolder.setPreferredSize(new Dimension(screenWidth / 3 + 210, innerPanelSize / 2));
		innerButtonHolder.setMinimumSize(new Dimension(screenWidth / 3 + 210, innerPanelSize / 2));
		innerButtonHolder.setBackground(Color.decode("#404040"));
		buttonHolder.add(innerButtonHolder, aConstraint);

		aConstraint = components.conditionalConstraints(1, 1, 2, 0, GridBagConstraints.NONE);
		
		aConstraint.anchor = GridBagConstraints.LINE_END;
		aConstraint.gridwidth = 1;
		JPanel speakerTwo = createSpeaker();
		controlPanel.add(speakerTwo, aConstraint);
	}

	public void addFeatureTabs() throws InvalidMidiDataException {
		JTabbedPane getFeatures = FeatureTabs.getInstance().createTabbedBar();
		
		aConstraint = components.conditionalConstraints(1, 1, 0, 0,	GridBagConstraints.NONE);
		aConstraint.anchor = GridBagConstraints.FIRST_LINE_START;
		aConstraint.gridwidth =1;
		contentPane.add(getFeatures, aConstraint);
	}

	public void addScreenPrompt() throws InvalidMidiDataException {
		
		aConstraint = components.conditionalConstraints(1, 1, 0, 0,	GridBagConstraints.NONE);
		aConstraint.anchor = GridBagConstraints.FIRST_LINE_END;
		aConstraint.gridwidth =1;
		
		
		originalScreenPrompt = ScreenPrompt.getInstance().createCurrentPromptState();
		contentPane.add(originalScreenPrompt, aConstraint);
	}

	public void updateScreenPrompt() throws InvalidMidiDataException {	
		aConstraint = components.conditionalConstraints(1, 1, 0, 0,GridBagConstraints.NONE);
		aConstraint.anchor = GridBagConstraints.FIRST_LINE_END;
		aConstraint.gridwidth =1;

		contentPane.remove(originalScreenPrompt);
		originalScreenPrompt = ScreenPrompt.getInstance().createCurrentPromptState();
		contentPane.add(originalScreenPrompt, aConstraint);
		// Needed to show update of JPanel after remove to update
		contentPane.validate();
		contentPane.repaint();

	}
}