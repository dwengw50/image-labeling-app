import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.image.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import javax.swing.filechooser.*;

// This GUI program is a tool for labeling object-based images with information
// such as joints, bounding box, and tags.

// SUPPORTING FILES:
// The specific joint names can be modified via the txt file called "JointNames.txt"
// located in this program's directory. The joint locations are stored as x, y
// coordinates, with the origin being at the top left corner of the image. Distinctly
// colored dots are used to visually display the locations of the joints on the image.
// The specific colors can be modified via the txt file "JointDotColors.txt" located
// in this program's directory. Each line in "JointDotColors.txt" should be a color
// defined by rgb values, in that order. To avoid unexpected results, the number of
// colors defined in "JointDotColors.txt" should be the same as the number of joint
// names in "JointNames.txt". The desired tags can also be modified via the txt file
// "TagNames.txt" located in this program's directory. Please do not change any of
// the above .txt file names.
// The image file "SampleDogJointsKey.png" is used as a sample joints key for application
// users. Please do not remove, rename, or modify this image file.
// The image file "SampleDogLabelFile.png" is used as a sample .label file output for
// application users' reference. Please do not remove, rename, or modify this image file.

// All of the recorded labeled information is outputted to a JSON-format .label file
// with file name "<current image file name>" + ".label".
// ("image.jpeg" --> "image.jpeg.label")
// In the .label file, each joint's name and x, y coordinate is stored, each tag's
// name and corresponding input is stored, and the bounding box's top-left x, y 
// coordinate and its width and height is stored.
// Note: the origin is located at the TOP-LEFT corner of the image

public class LabelImage extends JFrame implements ActionListener, WindowListener, KeyListener {
   
   private String folderName; // Absolute pathname of current directory
   private String fileName; // Absolute pathname of current image file
   private File[] imgFiles; // Stores all of the image files in the current directory
   private int currImgFileIndex; // Denotes the index of the current image file within the imgFiles array of current directory's files
   private JSONObject overall; // Root JSONObject for .label file
   private JSONArray dataset; // Stores the confirmed joint JSONObjects in JSON format for output
   private JSONObject tags; // Stores the confirmed tags in JSON format for output
   private JSONObject boundingBox; // Stores the x, y coordinate, width, and height of bounding box in JSON format for output
   private int boxX1; // Top-left corner x coordinate of bounding box
   private int boxY1; // Top-left corner y coordinate of bounding box
   private int boxX2; // Bottom-right corner x coordinate of bounding box
   private int boxY2; // Bottom-right corner y coordinate of bounding box
   private Color boundingBoxColor; // Display color of the bounding box
   private JTextField boxX1Input; // Text field to display top left corner x coordinate of bounding box
   private JTextField boxY1Input; // Text field to display top left corner y coordinate of bounding box
   private JTextField boxX2Input; // Text field to display bottom right corner x coordinate of bounding box
   private JTextField boxY2Input; // Text field to display bottom right corner y coordinate of bounding box
   private JButton confirmBox; // Button to confirm drawn bounding box
   private JFrame openFile; // Separate UI frame used for opening files
   private ImageIcon img; // Used to display current image
   private DrawableLabel imgLabel; // UI component that displays an image that can be drawn on (DrawableLabel is nested class)
   private Container c; // Frame's body
   private JComboBox<String> joint; // Drop-down list of all of the selectable joint names
   private JTextField x; // Text field to display x coordinate of current selected joint
   private JTextField y; // Text field to display y coordinate of current selected joint
   private JButton confirm; // Button to confirm current selected joint x and y coordinate
   private JButton load; // Button to open a file chooser and select a new image to be labeled
   private JButton previous; // Button to open the previous image found alphabetically in the current directory
   private JButton next; // Button to open the next image found alphabetically in the current directory
   private JComboBox<String> tagNames; // Drop-down list of all of the selectable tag names
   private JTextField tagInput; // Text field for user to input text for selected tag name
   private JButton confirmTag; // Button to confirm current selected tag with user input
   private Map<String, Color> jointDotColors; // Maps each joint name to its corresponding dot color for displaying purposes
   private java.util.List<String> confirmedJoints; // Records all previously confirmed joint names
   private java.util.List<String> jointNamesList; // Stores a list of all of the possible joint names (read in from "JointNames.txt")
   private java.util.List<String> tagNamesList; // Stores a list of all of the possible tag names (read in from "TagNames.txt")
   private java.util.List<Color> jointColorsList; // Stores a list of all of the corresponding joint dot colors (read in from "JointDotColors.txt")
   public static final String[] EXTENSIONS = new String[] {"gif", "jpeg", "jpg", "png"}; // List of all valid file extensions
   public static final Font TITLE_FONT = new Font("TimesRoman", Font.BOLD, 14); // Universal title font
   
   // Sets up the GUI, laying out all of the components. Initializes above fields,
   // including reading in from all supporting files for joints, colors, and tags.
   // Also reads in and initializes data from .label file if found in directory.
   // Parameters:
   //    - String fileName: absolute pathname of current image to be labeled
   //    - String folderName: absolute pathname of current directory in which current
   //                         image is located
   //    - File[] imgFiles: all image files in current image's directory.
   public LabelImage(String fileName, String folderName, File[] imgFiles) throws IOException, ParseException {
      this.folderName = folderName;
      this.fileName = fileName;
      this.imgFiles = imgFiles;
      currImgFileIndex = 0;
      if (this.imgFiles != null) {
         Arrays.sort(this.imgFiles);
         // Determine the appropriate index of the current image within the sorted
         // array of image files of this directory.
         while (!((this.imgFiles[currImgFileIndex].getAbsolutePath()).equals(fileName))) {
            currImgFileIndex += 1;
         }
      }
      openFile = new JFrame();
      dataset = new JSONArray();
      tags = new JSONObject();
      boundingBox = new JSONObject();
      boundingBoxColor = Color.BLACK; // Default bounding box color is always black
      confirmedJoints = new ArrayList<String>();
      
      jointNamesList = new ArrayList<String>();
      File jointNames = new File("JointNames.txt");
      Scanner readJointNames = new Scanner(jointNames);
      // Read in all of the joint names from "JointNames.txt"
      while (readJointNames.hasNext()) {
         jointNamesList.add(readJointNames.next());
      }
      
      tagNamesList = new ArrayList<String>();
      File tagNamesFile = new File("TagNames.txt");
      Scanner readTagNames = new Scanner(tagNamesFile);
      // Read in all of the tag names from "TagNames.txt"
      while (readTagNames.hasNext()) {
         tagNamesList.add(readTagNames.next());
      }
      
      jointColorsList = new ArrayList<Color>();
      File jointColorsFile = new File("JointDotColors.txt");
      Scanner readJointColors = new Scanner(jointColorsFile);
      // Read in all of the rgb values from "JointDotColors.txt" and store them as 
      // colors in jointColorsList
      while (readJointColors.hasNextLine()) {
         String line = readJointColors.nextLine();
         Scanner readLine = new Scanner(line);
         int r = Integer.parseInt(readLine.next());
         int g = Integer.parseInt(readLine.next());
         int b = Integer.parseInt(readLine.next());
         jointColorsList.add(new Color(r, g, b));
      }
      
      // Display image using image icon within a modified label component that allows for drawing
      File labelFile = new File(fileName + ".label");
      img = new ImageIcon(fileName);
      imgLabel = new DrawableLabel(img); // DrawableLabel is nested class
      imgLabel.setOpaque(true);
      imgLabel.setBackground(new Color(204, 255, 255));
      
      // If a corresponding .label file can be found for the current image, parse all
      // .label file's data and store locally for potential modification
      if (labelFile.exists()) {
         JSONParser parser = new JSONParser();
         FileReader reader = new FileReader(labelFile);
         JSONObject ovr = (JSONObject) parser.parse(reader);
         JSONArray inputJoints = (JSONArray) ovr.get("Joints");
         
         // Add all joints from the .label file into the current dataset to be modified
         // or added to via further labeling.
         for (Object inputJoint : inputJoints) {
            dataset.add((JSONObject) inputJoint);
            confirmedJoints.add("" + ((JSONObject) inputJoint).get("name"));
         }
         
         // Add all tags and their info from the .label file into the current dataset
         // of tags/info to be modified or added to via further labeling.
         JSONObject inputTags = (JSONObject) ovr.get("Tags");
         for (Iterator iterator = inputTags.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            tags.put(key, inputTags.get(key));
         }
         
         // Add info about bounding box from .label file into current dataset of
         // bounding box to be potentially modified. If no bounding box info is found,
         // the current box's info will just be stored as 0's for x, y, w, h.
         JSONObject inputBox = (JSONObject) ovr.get("Bounding box");
         int boxW = 0;
         int boxH = 0;
         int boxX = 0;
         int boxY = 0;
         for (Iterator iterator = inputBox.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            if (key.equals("x")) {
               boxX = Integer.parseInt("" + inputBox.get("x"));
               boundingBox.put("x", "" + inputBox.get("x"));
            }
            if (key.equals("y")) {
               boxY = Integer.parseInt("" + inputBox.get("y"));
               boundingBox.put("y", "" + inputBox.get("y"));
            }
            if (key.equals("w")) {
               boxW = Integer.parseInt("" + inputBox.get("w"));
               boundingBox.put("w", "" + inputBox.get("w"));
            }
            if (key.equals("h")) {
               boxH = Integer.parseInt("" + inputBox.get("h"));
               boundingBox.put("h", "" + inputBox.get("h"));
            }
         }
         boxX1 = boxX;
         boxY1 = boxY;
         boxX2 = boxX + boxW;
         boxY2 = boxY + boxH;
      }
      
      overall = new JSONObject();
      
      // Create map between joint names and dot colors for convenience
      jointDotColors = new HashMap<String, Color>();
      for (int i = 0; i < jointNamesList.size(); i++) {
         jointDotColors.put(jointNamesList.get(i), jointColorsList.get(i));
      }
      
      // Set up GUI
      setTitle("Label Image");
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      c = this.getContentPane();
      c.setLayout(new BorderLayout());
      c.add(imgLabel, BorderLayout.CENTER);
      JPanel menu = new JPanel();
      menu.setLayout(new GridLayout(4, 1));
      JPanel jointInfo = new JPanel();
      jointInfo.setLayout(new GridLayout(1, 2));
      JLabel jointInstruct = new JLabel("Joint name:", SwingConstants.RIGHT);
      jointInstruct.setFont(TITLE_FONT);
      jointInstruct.setOpaque(true);
      jointInstruct.setBackground(new Color(204, 255, 204));
      String[] jointNamesListArray = new String[jointNamesList.size()];
      // Convert to array for JComboBox constructor
      for (int i = 0; i < jointNamesList.size(); i++) {
         jointNamesListArray[i] = jointNamesList.get(i);
      }
      joint = new JComboBox<String>(jointNamesListArray);
      ComboBoxRenderer renderer = new ComboBoxRenderer(joint);
      // ComboBoxRenderer (nested class) is used to set each drop-down list's joint
      // name text color to be that of its corresponding dot
      renderer.setColors(jointColorsList);
      renderer.setStrings(jointNamesList);
      joint.setRenderer(renderer);
      joint.addActionListener(new ActionListener() {
         // When the selected item in the JComboBox is changed, change the x and y 
         // coordinate text fields to display the corresponding x and y coordinates
         // of the newly selected joint. If the selected joint has not been confirmed,
         // the text fields will be empty.
         public void actionPerformed(ActionEvent e) {
            String curr = "" + joint.getSelectedItem();
            if (confirmedJoints.contains(curr)) {
               JSONObject desired = null;
               for (Object o : dataset) {
                  if (("" + ((JSONObject)o).get("name")).equals(curr)) {
                     desired = (JSONObject)o;
                     break;
                  }
               }
               x.setText("" + desired.get("x-coordinate"));
               y.setText("" + desired.get("y-coordinate"));
            } else {
               x.setText("");
               y.setText("");
            }
         }
      });
      joint.addKeyListener(this);
      jointInfo.add(jointInstruct);
      jointInfo.add(joint);
      menu.add(jointInfo);
      JPanel xInfo = new JPanel();
      xInfo.setLayout(new GridLayout(1, 2));
      JLabel xCoord = new JLabel("X coordinate:", SwingConstants.RIGHT);
      xCoord.setOpaque(true);
      xCoord.setBackground(new Color(204, 255, 204));
      x = new JTextField();
      xInfo.add(xCoord);
      xInfo.add(x);
      menu.add(xInfo);
      JPanel yInfo = new JPanel();
      yInfo.setLayout(new GridLayout(1, 2));
      JLabel yCoord = new JLabel("Y coordinate:", SwingConstants.RIGHT);
      yCoord.setOpaque(true);
      yCoord.setBackground(new Color(204, 255, 204));
      y = new JTextField();
      
      // Since default selected item is the first joint, see if that joint has been
      // confirmed with x, y coordinates before. If so, display the x, y coordinates.
      if (confirmedJoints.contains("" + jointNamesList.get(0))) {
         JSONObject desired = null;
         for (Object o : dataset) {
            if (("" + ((JSONObject)o).get("name")).equals("" + jointNamesList.get(0))) {
               desired = (JSONObject)o;
               break;
            }
         }
         x.setText("" + desired.get("x-coordinate"));
         y.setText("" + desired.get("y-coordinate"));
      }
      
      yInfo.add(yCoord);
      yInfo.add(y);
      menu.add(yInfo);
      confirm = new JButton("CONFIRM JOINT AND INCREMENT");
      confirm.addActionListener(this);
      confirm.setOpaque(true);
      confirm.setBackground(new Color(102, 255, 178));
      confirm.addKeyListener(this);
      menu.add(confirm);
      menu.setBackground(new Color(204, 255, 204));
      load = new JButton("LOAD NEW IMAGE");
      previous = new JButton("(<) PREVIOUS IMAGE");
      next = new JButton("NEXT IMAGE (>)");
      load.addActionListener(this);
      load.addKeyListener(this);
      previous.addActionListener(this);
      previous.addKeyListener(this);
      next.addActionListener(this);
      next.addKeyListener(this);
      JPanel top = new JPanel();
      top.setBackground(new Color(204, 229, 255));
      top.setLayout(new GridLayout(3, 1));
      JLabel imgName = new JLabel(fileName, SwingConstants.CENTER);
      imgName.setOpaque(true);
      imgName.setBackground(new Color(204, 229, 255));
      top.add(imgName);
      top.add(load);
      JPanel prevAndNext = new JPanel();
      prevAndNext.setLayout(new GridLayout(1, 2));
      if (currImgFileIndex > 0) {
         prevAndNext.add(previous);
      } else { // If the current image is the front of the imgFiles array, do not include a prev button
         JLabel noPrev = new JLabel("No previous image found", SwingConstants.CENTER);
         noPrev.setOpaque(true);
         noPrev.setBackground(new Color(204, 229, 255));
         prevAndNext.add(noPrev);
      }
      if (currImgFileIndex < imgFiles.length - 1) {
         prevAndNext.add(next);
      } else { // If the current image is the end of the imgFiles array, do not include a next button
         JLabel noNext = new JLabel("No next image found", SwingConstants.CENTER);
         noNext.setOpaque(true);
         noNext.setBackground(new Color(204, 229, 255));
         prevAndNext.add(noNext);
      }
      top.add(prevAndNext);
      JPanel rightMenu = new JPanel();
      rightMenu.setLayout(new GridLayout(8, 1));
      JPanel tagInfo = new JPanel();
      tagInfo.setBackground(new Color(255, 204, 153));
      tagInfo.setLayout(new GridLayout(1, 3));
      JLabel tagInstruct = new JLabel("Tags:", SwingConstants.CENTER);
      tagInstruct.setFont(TITLE_FONT);
      tagInstruct.setOpaque(true);
      tagInstruct.setBackground(new Color(255, 204, 153));
      // Convert to array for JComboBox constructor
      String[] tagNamesListArray = new String[tagNamesList.size()];
      for (int j = 0; j < tagNamesList.size(); j++) {
         tagNamesListArray[j] = tagNamesList.get(j);
      }
      tagNames = new JComboBox<String>(tagNamesListArray);
      tagInput = new JTextField();
      tagNames.addActionListener(new ActionListener() {
         // if the selected tag is changed, also change the text field to display
         // the corresponding information for the newly selected tag. If no previously
         // stored info can be found for the given tag, display an empty text field.
         public void actionPerformed(ActionEvent e) {
            String curr = "" + tagNames.getSelectedItem();
            if (tags.keySet().contains(curr)) {
               tagInput.setText("" + tags.get(curr));
            } else {
               tagInput.setText("");
            }
         }
      });
      tagNames.addKeyListener(this);
      
      // Since the default tag is always the first one, see if it has previously stored
      // info. If so, display info in text field.
      if (tags.keySet().contains(tagNamesList.get(0))) {
         tagInput.setText("" + tags.get(tagNamesList.get(0)));
      }
      
      tagInfo.add(tagInstruct);
      tagInfo.add(tagNames);
      tagInfo.add(tagInput);
      rightMenu.add(tagInfo);
      confirmTag = new JButton("CONFIRM TAG");
      confirmTag.setOpaque(true);
      confirmTag.setBackground(new Color(255, 153, 51));
      confirmTag.addActionListener(this);
      confirmTag.addKeyListener(this);
      rightMenu.add(confirmTag);
      JLabel boxTitle = new JLabel("--------Bounding box:--------", SwingConstants.CENTER);
      boxTitle.setFont(TITLE_FONT);
      boxTitle.setOpaque(true);
      boxTitle.setBackground(new Color(255, 204, 204));
      rightMenu.add(boxTitle);
      
      // Allow the user to choose a display color (black or white) for the bounding box.
      // This color info will not be saved in any way for future re-labeling.
      JPanel boxColorOptions = new JPanel();
      boxColorOptions.setOpaque(true);
      boxColorOptions.setBackground(new Color(255, 204, 204));
      boxColorOptions.setLayout(new FlowLayout());
      JLabel descColorOption = new JLabel("Box Color:", SwingConstants.RIGHT);
      boxColorOptions.add(descColorOption);
      ButtonGroup boxColors = new ButtonGroup();
      JRadioButton blackOption = new JRadioButton("Black", true); // Default with black selected
      boxColors.add(blackOption);
      blackOption.addActionListener(new ActionListener() {
         // if the black option is selected, set the bounding box color to be black and
         // redisplay with black color box
         public void actionPerformed(ActionEvent e) {
            if (blackOption.isSelected()) {
               boundingBoxColor = Color.BLACK;
               imgLabel.repaint();
            }
         }
      });
      blackOption.addKeyListener(this);
      JRadioButton whiteOption = new JRadioButton("White", false);
      boxColors.add(whiteOption);
      whiteOption.addActionListener(new ActionListener() {
         // if the white option is selected, set the bounding box color to be white and
         // redisplay with white color box
         public void actionPerformed(ActionEvent e) {
            if (whiteOption.isSelected()) {
               boundingBoxColor = Color.WHITE;
               imgLabel.repaint();
            }
         }
      });
      whiteOption.addKeyListener(this);
      boxColorOptions.add(blackOption);
      boxColorOptions.add(whiteOption);
      rightMenu.add(boxColorOptions);
      
      // Further GUI setup
      JLabel boxInstruct = new JLabel("<html><center>Use right mouse-button to drag a new box.<br>Resize drawn box by dragging corners with right mouse-button.</center></html>", SwingConstants.CENTER);
      boxInstruct.setOpaque(true);
      boxInstruct.setBackground(new Color(255, 204, 204));
      rightMenu.add(boxInstruct);
      JPanel box1Info = new JPanel();
      box1Info.setOpaque(true);
      box1Info.setBackground(new Color(255, 204, 204));
      box1Info.setLayout(new FlowLayout());
      box1Info.add(new JLabel("Top Left:", SwingConstants.CENTER));
      box1Info.add(new JLabel("x:", SwingConstants.RIGHT));
      boxX1Input = new JTextField();
      boxX1Input.setText("" + boxX1);
      boxX1Input.setColumns(4);
      box1Info.add(boxX1Input);
      box1Info.add(new JLabel("y:", SwingConstants.RIGHT));
      boxY1Input = new JTextField();
      boxY1Input.setText("" + boxY1);
      boxY1Input.setColumns(4);
      box1Info.add(boxY1Input);
      rightMenu.add(box1Info);
      JPanel box2Info = new JPanel();
      box2Info.setOpaque(true);
      box2Info.setBackground(new Color(255, 204, 204));
      box2Info.setLayout(new FlowLayout());
      box2Info.add(new JLabel("Bottom Right:", SwingConstants.CENTER));
      box2Info.add(new JLabel("x:", SwingConstants.RIGHT));
      boxX2Input = new JTextField();
      boxX2Input.setText("" + boxX2);
      boxX2Input.setColumns(4);
      box2Info.add(boxX2Input);
      box2Info.add(new JLabel("y:", SwingConstants.RIGHT));
      boxY2Input = new JTextField();
      boxY2Input.setText("" + boxY2);
      boxY2Input.setColumns(4);
      box2Info.add(boxY2Input);
      rightMenu.add(box2Info);
      confirmBox = new JButton("CONFIRM BOX");
      confirmBox.setOpaque(true);
      confirmBox.setBackground(new Color(255, 102, 102));
      rightMenu.add(confirmBox);
      confirmBox.addActionListener(this);
      confirmBox.addKeyListener(this);
      
      // Setup bottom sample key
      JPanel bottomKey = new JPanel();
      bottomKey.setLayout(new BorderLayout());
      bottomKey.setOpaque(true);
      bottomKey.setBackground(new Color(153, 204, 255));
      ImageIcon sampleKeyImage = new ImageIcon("SampleDogJointsKey.png");
      JLabel sampleKeyImgLabel = new JLabel(sampleKeyImage);
      sampleKeyImgLabel.setOpaque(true);
      sampleKeyImgLabel.setBackground(new Color(153, 204, 255));
      bottomKey.add(sampleKeyImgLabel, BorderLayout.LINE_START);
      JLabel sampleTitle = new JLabel("REFERENCE JOINT KEY AND SAMPLE LABEL FILE:");
      sampleTitle.setFont(TITLE_FONT);
      sampleTitle.setOpaque(true);
      sampleTitle.setBackground(new Color(153, 204, 255));
      bottomKey.add(sampleTitle, BorderLayout.PAGE_START);
      ImageIcon sampleLabelFileImage = new ImageIcon("SampleDogLabelFile.png");
      JLabel sampleLabelFile = new JLabel(sampleLabelFileImage);
      sampleLabelFile.setOpaque(true);
      sampleLabelFile.setBackground(new Color(153, 204, 255));
      bottomKey.add(sampleLabelFile, BorderLayout.LINE_END);
      
      c.add(bottomKey, BorderLayout.PAGE_END);
      c.add(menu, BorderLayout.LINE_START);
      c.add(top, BorderLayout.PAGE_START);
      c.add(rightMenu, BorderLayout.LINE_END);
      
      // Listen for when frame is closed and when keyboard buttons are clicked
      addWindowListener(this);
      addKeyListener(this);
      
      pack();
      setVisible(true);
   }
   
   // Takes care of when ActionEvents are fired (JButtons are clicked). Case for each
   // of the 6 JButtons in the UI.
   // Parameters:
   //    - ActionEvent e: the action event that is fired whenever any button in the GUI with an associated action listener is clicked
   public void actionPerformed(ActionEvent e) {
      String source = ((JButton)e.getSource()).getActionCommand(); // Get the name of the JButton that was clicked
      // Confirm Joint button was clicked: (and an x and y coordinate have been selected)
      if (source.equals("CONFIRM JOINT AND INCREMENT") && !x.getText().isEmpty() && !y.getText().isEmpty()) {
         // if the selected joint has been previously confirmed:
         if (confirmedJoints.contains("" + joint.getSelectedItem())) {
            // Look for selected joint in the dataset of confirmed joints, storing the index
            int desiredIndex = 0;
            for (int i = 0; i < dataset.size(); i++) {
               JSONObject curr = (JSONObject) dataset.get(i);
               if (curr.get("name").equals("" + joint.getSelectedItem())) {
                  desiredIndex = i;
                  break;
               }
            }
            // Access the desired joint JSONObject and modify its x and y coordinates
            ((JSONObject)dataset.get(desiredIndex)).put("x-coordinate", Integer.parseInt(x.getText()));
            ((JSONObject)dataset.get(desiredIndex)).put("y-coordinate", Integer.parseInt(y.getText()));
         } else { // selected joint has not been previously confirmed:
            // Create a brand new joint JSONObject for this joint and add to the dataset
            JSONObject newJoint = new JSONObject();
            newJoint.put("name", "" + joint.getSelectedItem());
            newJoint.put("x-coordinate", Integer.parseInt(x.getText()));
            newJoint.put("y-coordinate", Integer.parseInt(y.getText()));
            dataset.add(newJoint);
            confirmedJoints.add("" + joint.getSelectedItem());
         }
         
         // redisplay image with updated colored dots
         imgLabel.repaint();
         
         // Auto-increment the selected joint to be the next in the drop-down menu for quick labeling
         String currSelected = "" + joint.getSelectedItem();
         int currSelectedIndex = jointNamesList.indexOf(currSelected);
         if (currSelectedIndex < jointNamesList.size() - 1) {
            joint.setSelectedIndex(currSelectedIndex + 1);
         }
         
      } else if (source.equals("LOAD NEW IMAGE")) { // Load New Image button was clicked
         setVisible(false);
         c.removeAll();
         
         // if at least one joint has been confirmed, add all labeled info to root and write to .label file
         if (!dataset.isEmpty()) {
            try {
               PrintStream output = new PrintStream(fileName + ".label");
               overall.put("Joints", dataset);
               overall.put("Tags", tags);
               overall.put("Bounding box", boundingBox);
               output.println(overall.toJSONString());
            } catch (FileNotFoundException ex) {}
         }
         
         File folder = new File(folderName);
         // Set up the file chooser with the current directory
         JFileChooser chooser = new JFileChooser();
         if (folder.isDirectory()) {
            chooser.setCurrentDirectory(folder);
         }
         // Filter out all non-image files
         FileNameExtensionFilter extFilter = new FileNameExtensionFilter("image", "jpeg", "jpg", "png", "gif");
         chooser.setFileFilter(extFilter);
         // Allow directories and files to be chosen
         chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
         // Open file chooser
         int returnVal = chooser.showOpenDialog(openFile);
         openFile.pack();
         openFile.setVisible(true);
         String newFileName = "";
         String newFolderName = "";
         File[] newImgFiles = null;
         
         // Used for filtering out non-image files when creating the new array of
         // image files for the new image
         FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
               File f = new File(name);
               for (String ext : EXTENSIONS) {
                  if (name.endsWith(ext) && !f.isDirectory()) {
                     return true;
                  }
               }
               return false;
            }
         };
         
         // When a file/directory is chosen:
         if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected.isDirectory()) { // User selected a directory
               newFolderName = selected.getAbsolutePath();
               newImgFiles = selected.listFiles(filter);
               if (newImgFiles.length == 0) { // Exit program if selected directory has no image files
                  JOptionPane.showMessageDialog(openFile, "No image files found in selected directory... Exiting");
                  openFile.setVisible(false);
                  System.exit(0);
               }
               Arrays.sort(newImgFiles);
               newFileName = newImgFiles[0].getAbsolutePath(); // Set new image file to be the first image
                                                               // in selected directory
            } else { // User selected a file (not directory)
               newFileName = selected.getAbsolutePath();
               File parent = selected.getParentFile();
               if (parent != null) { // Set new folder to be parent directory of selected image file
                  newFolderName = parent.getAbsolutePath();
                  // List and sort all image files in parent directory
                  newImgFiles = parent.listFiles(filter);
                  Arrays.sort(newImgFiles);
               }
            }
            openFile.setVisible(false);
            openFile.removeAll();
         } else { // Exit program if a file/directory is not chosen (user manually exits from file chooser window)
            System.exit(0);
         }
         try {
            // Create new image labeling GUI for new selected image
            LabelImage main = new LabelImage(newFileName, newFolderName, newImgFiles);
         } catch (IOException ex) {
         } catch (ParseException pEx) {}
      } else if (source.equals("(<) PREVIOUS IMAGE")) { // Previous Image button was clicked
         if (currImgFileIndex > 0) { // as long as not the very first image file alphabetically in directory
            setVisible(false);
            c.removeAll();
            
            // if at least one joint has been confirmed, add all labeled info to root and write to .label file
            if (!dataset.isEmpty()) { 
               try {
                  PrintStream output = new PrintStream(fileName + ".label");
                  overall.put("Joints", dataset);
                  overall.put("Tags", tags);
                  overall.put("Bounding box", boundingBox);
                  output.println(overall.toJSONString());
               } catch (FileNotFoundException ex) {}
            }
            
            // Get the previous image file alphabetically in the current image's parent directory
            String newFileName = imgFiles[currImgFileIndex - 1].getAbsolutePath();
            try {
               // Create new image labeling GUI for new image
               LabelImage main = new LabelImage(newFileName, folderName, imgFiles);
            } catch (IOException ex) {
            } catch (ParseException pEx) {}
         }
      } else if (source.equals("NEXT IMAGE (>)")) { // Next Image button was clicked
         if (currImgFileIndex < imgFiles.length - 1) { // as long as not the very last image file alphabetically in directory
            setVisible(false);
            c.removeAll();
            
            // if at least one joint has been confirmed, add all labeled info to root and write to .label file
            if (!dataset.isEmpty()) {
               try {
                  PrintStream output = new PrintStream(fileName + ".label");
                  overall.put("Joints", dataset);
                  overall.put("Tags", tags);
                  overall.put("Bounding box", boundingBox);
                  output.println(overall.toJSONString());
               } catch (FileNotFoundException ex) {}
            }
            
            // Get the next image file alphabetically in the current image's parent directory
            String newFileName = imgFiles[currImgFileIndex + 1].getAbsolutePath();
            try {
               // Create a new image labeling GUI for new image
               LabelImage main = new LabelImage(newFileName, folderName, imgFiles);
            } catch (IOException ex) {
            } catch (ParseException pEx) {}
         }
      } else if (source.equals("CONFIRM TAG")) { // Confirm Tag button was clicked
         // Get user input text from tag text field
         String currTagInput = "" + tagInput.getText();
         
         // As long as input text is not empty, save input text with selected tag description into tag JSONObject
         if (!currTagInput.equals("")) {
            String currTagName = "" + tagNames.getSelectedItem();
            tags.put(currTagName, currTagInput);
         }
      } else if (source.equals("CONFIRM BOX")) { // Confirm Box button was clicked
         // Save the drawn bounding box's x, y top-left coordinates and width/height into bounding box JSONObject
         boundingBox.put("x", "" + boxX1);
         boundingBox.put("y", "" + boxY1);
         boundingBox.put("w", "" + (boxX2 - boxX1));
         boundingBox.put("h", "" + (boxY2 - boxY1));
      }
   }
   
   // Below empty methods to satisfy implemented WindowListener interface
   public void windowActivated(WindowEvent e) {}
   
   public void windowDeactivated(WindowEvent e) {}
   
   public void windowDeiconified(WindowEvent e) {}
   
   public void windowIconified(WindowEvent e) {}
   
   public void windowOpened(WindowEvent e) {}
   
   // If the current image labeling GUI is manually closed by user, and at least one joint has been confirmed,
   // add all labeled image data into root JSONObject and output to .label file
   // Parameters:
   //    - WindowEvent e: action event that is fired when the window is closed
   public void windowClosed(WindowEvent e) {
      if (!dataset.isEmpty()) {
         try {
            PrintStream output = new PrintStream(fileName + ".label");
            overall.put("Joints", dataset);
            overall.put("Tags", tags);
            overall.put("Bounding box", boundingBox);
            output.println(overall.toJSONString());
         } catch (FileNotFoundException ex) {}
      }
   }
   
   // If the current image labeling GUI is in the process of closing, and at least one joint has been confirmed,
   // add all labeled image data into root JSONObject and output to .label file
   // Parameters:
   //    - WindowEvent e: action event that is fired when the window is closing
   public void windowClosing(WindowEvent e) {
      if (!dataset.isEmpty()) {
         try {
            PrintStream output = new PrintStream(fileName + ".label");
            overall.put("Joints", dataset);
            overall.put("Tags", tags);
            overall.put("Bounding box", boundingBox);
            output.println(overall.toJSONString());
         } catch (FileNotFoundException ex) {}
      }
   }
   
   // Empty keyPressed and keyTyped methods to satisfy KeyListener interface
   public void keyPressed(KeyEvent e) {}
   
   public void keyTyped(KeyEvent e) {}
   
   // Handles when right or left arrow keyboard buttons are released (pressed and then released)
   // Parameters:
   //    - KeyEvent e: key event that is fired whenever a keyboard button is released (clicked)
   public void keyReleased(KeyEvent e) {
      // If right arrow button, move to next image by programatically clicking the Next Image button.
      // Likewise for left arrow button.
      if (e.getKeyCode() == KeyEvent.VK_RIGHT && currImgFileIndex < imgFiles.length - 1) {
         next.doClick();
      } else if (e.getKeyCode() == KeyEvent.VK_LEFT && currImgFileIndex > 0) {
         previous.doClick();
      }
   }
   
   // Main method to handle initial application startup
   public static void main(String[] args) throws IOException, ParseException {
   
      // Create initial file chooser
      JFrame initialOpenFile = new JFrame();
      JFileChooser initialChooser = new JFileChooser();
      FileNameExtensionFilter initialExtensionFilter = new FileNameExtensionFilter("image", "jpeg", "jpg", "png", "gif");
      initialChooser.setFileFilter(initialExtensionFilter); // Filter out all non-image files
      initialChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); // Allow both directories and files to be selected
      int returnVal = initialChooser.showOpenDialog(initialOpenFile);
      initialOpenFile.pack();
      initialOpenFile.setVisible(true);
      
      String initFolderName = "";
      String initFileName = "";
      File[] initImgFiles = null;
      
      // File filter to filter out non-image files. To be used when listing all directory image files into an array.
      FilenameFilter initialFilter = new FilenameFilter() {
         @Override
         public boolean accept(File dir, String name) {
            File f = new File(name);
            for (String ext : EXTENSIONS) {
               if (name.endsWith(ext) && !f.isDirectory()) {
                  return true;
               }
            }
            return false;
         }
      };
      
      // Once a file/directory has been selected, determine if selected item is file or directory
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         File selected = initialChooser.getSelectedFile();
         if (selected.isDirectory()) { // If selected a directory
            // If the chosen directory has image file(s), initial folder is chosen directory,
            // initial array of image files is the alphabetically sorted list of all image files
            // in the chosen directory, and initial image file is the first file alphabetically
            // in the chosen directory.
            initFolderName = selected.getAbsolutePath();
            initImgFiles = selected.listFiles(initialFilter);
            if (initImgFiles.length == 0) { // If chosen directory has no image files, display error and exit application
               JOptionPane.showMessageDialog(initialOpenFile, "No image files found in selected directory... Exiting");
               initialOpenFile.setVisible(false);
               System.exit(0);
            }
            Arrays.sort(initImgFiles);
            initFileName = initImgFiles[0].getAbsolutePath();
         } else { // Otherwise, if selected a file
            // Initial image file is chosen file, initial directory is chosen file's parent directory,
            // and initial array of image files is the alphabetically sorted list of all image files in
            // the chosen file's parent directory. If the chosen file has no parent directory, the
            // initial array of image files remains empty.
            initFileName = selected.getAbsolutePath();
            File parent = selected.getParentFile();
            if (parent != null) {
               initFolderName = parent.getAbsolutePath();
               initImgFiles = parent.listFiles(initialFilter);
               Arrays.sort(initImgFiles);
            }
         }
         initialOpenFile.setVisible(false);
         initialOpenFile.removeAll();
      } else { // if user does not select anything, exit application
         System.exit(0);
      }
      
      // Create an initial image labeling GUI for the initial image file
      LabelImage main = new LabelImage(initFileName, initFolderName, initImgFiles);
   }
   
   // Nested class for allowing an image to be drawn on via mouse actions in the GUI
   class DrawableLabel extends JLabel {
      
      private int mouseX; // X-position of mouse click
      private int mouseY; // Y-position of mouse click
      private int mouseXStart; // Stored x-position when mouse is first pressed and about to be dragged
      private int mouseYStart; // Stored y-position when mouse is first pressed and about to be dragged
      private boolean resizingTopLeft; // True when the top left corner of the bounding box is being resized
      private boolean resizingBottomRight; // True when the bottom right corner of the bounding box is being resized
      private boolean resizingTopRight; // True when the top right corner of the bounding box is being resized
      private boolean resizingBottomLeft; // True when the bottom left corner of the bounding box is being resized
      private int axisX; // Used to store the non-moving x-position corner when the opposite corner
                         // of the bounding box is being resized/dragged
      private int axisY; // Used to store the non-moving y-position corner when the opposite corner
                         // of the bounding box is being resized/dragged
      private boolean mouseJointClicked;
      
      // Creates a new drawable image component with the specified image as a basis
      // Parameters:
      //    - ImageIcon img: the image icon to be displayed as the basis of this component
      public DrawableLabel(ImageIcon img) {
         super(img); // add image to component
         
         // Initially not resizing
         resizingTopLeft = false;
         resizingBottomRight = false;
         
         // Initially no mouse clicked
         mouseJointClicked = false;
         
         // Add a mouse listener to receive mouse-pressed and mouse-released actions
         this.addMouseListener(new MouseAdapter() {
            
            // When mouse is clicked on this component, if the mouse click button was the left mouse button,
            // store the x, y coordinates of mouse click for redisplaying, calculate and display the 
            // corresponding x, y position within the image itself, and redisplay the image with updated dots.
            // Parameters:
            //    - MouseEvent e: the mouse action event that is fired when the mouse is clicked
            public void mouseClicked(MouseEvent e) {
               if (e.getButton() == MouseEvent.BUTTON1) {
                  mouseX = e.getX();
                  mouseY = e.getY();
                  // Calculate x, y position with respect to image itself
                  int xC = mouseX - ((imgLabel.getWidth() - img.getIconWidth()) / 2);
                  int yC = mouseY - ((imgLabel.getHeight() - img.getIconHeight()) / 2);
                  x.setText("" + xC);
                  y.setText("" + yC);
                  mouseJointClicked = true;
                  repaint();
               }
            }
            
            // When mouse is pressed on this component, if the mouse pressed button was the right mouse button,
            // store mouse pressed x, y position with respect to the image. If the mouse was pressed within two
            // coordinates of any of the previously drawn bounding box's corners, set the appropriate resizing
            // corner boolean to be true and store the appropriate non-moving axis corner of the box when resizing.
            // Finally, redisplay with updated box.
            // Parameters:
            //    - MouseEvent e: the mouse action event that is fired when the mouse is pressed
            public void mousePressed(MouseEvent e) {
               if (e.getButton() == MouseEvent.BUTTON3) {
                  // Get mouse pressed x, y position with respect to image
                  mouseXStart = e.getX() - ((imgLabel.getWidth() - img.getIconWidth()) / 2);
                  mouseYStart = e.getY() - ((imgLabel.getHeight() - img.getIconHeight()) / 2);
                  
                  // If within two coordinates of any of the four previously drawn box's corners, begin resizing.
                  if ((mouseXStart - 2 == boxX1 || mouseXStart - 1 == boxX1 || mouseXStart == boxX1 || mouseXStart + 2 == boxX1
                      || mouseXStart + 1 == boxX1) && (mouseYStart - 2 == boxY1 || mouseYStart - 1 == boxY1 || mouseYStart == boxY1
                      || mouseYStart + 1 == boxY1 || mouseYStart + 2 == boxY1)) { // Pressed at top-left corner
                     resizingTopLeft = true; // Resizing top-left mode is activated
                     // Non-moving axis when resizing/dragging is the opposite corner (bottom-right)
                     axisX = boxX2;
                     axisY = boxY2;
                  } else if ((mouseXStart - 2 == boxX2 || mouseXStart - 1 == boxX2 || mouseXStart == boxX2 || mouseXStart + 2 == boxX2
                             || mouseXStart + 1 == boxX2) && (mouseYStart - 2 == boxY2 || mouseYStart - 1 == boxY2 || mouseYStart == boxY2
                             || mouseYStart + 1 == boxY2 || mouseYStart + 2 == boxY2)) { // Pressed at bottom-right corner
                     resizingBottomRight = true; // Resizing bottom-right mode is activated
                     // Non-moving axis when resizing/dragging is the opposite corner (top-left)
                     axisX = boxX1;
                     axisY = boxY1;
                  } else if ((mouseXStart - 2 == boxX2 || mouseXStart - 1 == boxX2 || mouseXStart == boxX2 || mouseXStart + 2 == boxX2
                             || mouseXStart + 1 == boxX2) && (mouseYStart - 2 == boxY1 || mouseYStart - 1 == boxY1 || mouseYStart == boxY1
                             || mouseYStart + 1 == boxY1 || mouseYStart + 2 == boxY1)) { // Pressed at top-right corner
                     resizingTopRight = true; // Resizing top-right mode is activated
                     // Non-moving axis when resizing/dragging is the opposite corner (bottom-left)
                     axisX = boxX1;
                     axisY = boxY2;
                  } else if ((mouseXStart - 2 == boxX1 || mouseXStart - 1 == boxX1 || mouseXStart == boxX1 || mouseXStart + 2 == boxX1
                             || mouseXStart + 1 == boxX1) && (mouseYStart - 2 == boxY2 || mouseYStart - 1 == boxY2 || mouseYStart == boxY2
                             || mouseYStart + 1 == boxY2 || mouseYStart + 2 == boxY2)) { // Pressed at bottom-left corner
                     resizingBottomLeft = true; // Resizing bottom-left mode is activated
                     // Non-moving axis when resizing/dragging is the opposite corner (top-right)
                     axisX = boxX2;
                     axisY = boxY1;
                  }
                  
                  // Redisplay
                  repaint();
               }
            }
            
            // When the mouse is released on this component, and the mouse button being released is the right mouse-button,
            // Get the mouse released x, y position with respect to the image. If any of the resizing modes are activated,
            // use the mouse released position and the stored non-moving axis/corner to find and store the new x, y positions
            // of the new box. Otherwise, store the x, y positions of the new box using the starting mouse pressed and ending
            // mouse released x, y positions. Display these updated box x, y positions, redisplay component with updated box, 
            // and deactivate all resizing modes.
            // Parameters:
            //    - MouseEvent e: the mouse action event that is fired when the mouse is released
            public void mouseReleased(MouseEvent e) {
               if (e.getButton() == MouseEvent.BUTTON3) {
                  // Get mouse released x, y position with respect to the image
                  int mouseXEnd = e.getX() - ((imgLabel.getWidth() - img.getIconWidth()) / 2);
                  int mouseYEnd = e.getY() - ((imgLabel.getHeight() - img.getIconHeight()) / 2);
                  
                  // If any of the resizing modes are activated, the new box's top-left and bottom-right x, y positions
                  // can be calculated by comparing the mouse-released x, y position with the previously determined
                  // non-moving axis/corner of the box.
                  if (resizingTopLeft || resizingBottomRight || resizingTopRight || resizingBottomLeft) {
                     boxX1 = Math.min(mouseXEnd, axisX);
                     boxY1 = Math.min(mouseYEnd, axisY);
                     boxX2 = Math.max(mouseXEnd, axisX);
                     boxY2 = Math.max(mouseYEnd, axisY);
                  } else { // If not resizing, the new box's top-left and bottom-right x, y positions can be calculated
                           // by comparing the previously determined initial mouse-pressed x, y position with the mouse-
                           // released x, y position.
                     boxX1 = Math.min(mouseXStart, mouseXEnd);
                     boxY1 = Math.min(mouseYStart, mouseYEnd);
                     boxX2 = Math.max(mouseXStart, mouseXEnd);
                     boxY2 = Math.max(mouseYStart, mouseYEnd);
                  }
                  
                  // Display the updated box corner x, y positions
                  boxX1Input.setText("" + boxX1);
                  boxY1Input.setText("" + boxY1);
                  boxX2Input.setText("" + boxX2);
                  boxY2Input.setText("" + boxY2);
                  
                  // Redisplay with updated box
                  repaint();
                  
                  // Deactivate all resizing modes
                  resizingTopLeft = false;
                  resizingTopRight = false;
                  resizingBottomRight = false;
                  resizingBottomLeft = false;
               }
            }
         });
         
         // add a mouse motion listener to receive mouse-dragged actions
         this.addMouseMotionListener(new MouseMotionAdapter() {
         
            // When the mouse is dragged (button is held down and mouse is moved), if the right mouse button is dragged,
            // determine current mouse position with respect to the image. If any resizing mode is activated, use the
            // current mouse position with the previously determined non-moving axis/corner of the box to determine the
            // new box's x, y positions. Otherwise, use the starting mouse position (when the mouse is first pressed to be
            // dragged) and the current mouse position to determine the new box's x, y positions. Display updated box
            // positions and redisplay updated box.
            // Parameters: 
            //    - MouseEvent e: the mouse action event that is fired repeatedly as the mouse is dragged.
            public void mouseDragged(MouseEvent e) {
               if (SwingUtilities.isRightMouseButton(e)) {
                  // Determine current x, y mouse position with respect to the image
                  int mouseXEnd = e.getX() - ((imgLabel.getWidth() - img.getIconWidth()) / 2);
                  int mouseYEnd = e.getY() - ((imgLabel.getHeight() - img.getIconHeight()) / 2);
                  
                  // If any of the resizing modes are activated, determine the new box's top-left and bottom-right
                  // x, y positions by comparing the x, y positions of the above current mouse position with the 
                  // previously determined non-moving axis/corner of the box.
                  if (resizingTopLeft || resizingBottomRight || resizingTopRight || resizingBottomLeft) {
                     boxX1 = Math.min(mouseXEnd, axisX);
                     boxY1 = Math.min(mouseYEnd, axisY);
                     boxX2 = Math.max(mouseXEnd, axisX);
                     boxY2 = Math.max(mouseYEnd, axisY);
                  } else { // If not resizing, the new box's top-left and bottom-right x, y positions can be calculated
                           // by comparing the previously determined initial mouse-pressed x, y position with the current
                           // mouse x, y position.
                     boxX1 = Math.min(mouseXStart, mouseXEnd);
                     boxY1 = Math.min(mouseYStart, mouseYEnd);
                     boxX2 = Math.max(mouseXStart, mouseXEnd);
                     boxY2 = Math.max(mouseYStart, mouseYEnd);
                  }
                  
                  // Display the updated box corner x, y positions as the box is further dragged
                  boxX1Input.setText("" + boxX1);
                  boxY1Input.setText("" + boxY1);
                  boxX2Input.setText("" + boxX2);
                  boxY2Input.setText("" + boxY2);
                  
                  // Redisplay with updated dragging box
                  repaint();
               }
            }
         });
      }
      
      // Overrided method that redraws the component with joints and box, if previously confirmed.
      // Parameters:
      //    - Graphics g: the graphics tool used to draw on this component
      public void paintComponent(Graphics g) {
         super.paintComponent(g); // Draw the image
         
         // Determine offset between component's coordinate system and image's coordinate system.
         int xOffset = (this.getWidth() - img.getIconWidth()) / 2;
         int yOffset = (this.getHeight() - img.getIconHeight()) / 2;
         
         // Draw the bounding box using the chosen color (black/white) and the x, y positions of the
         // top-left and bottom-right corners.
         g.setColor(boundingBoxColor);
         g.drawRect(boxX1 + xOffset, boxY1 + yOffset, boxX2 - boxX1, boxY2 - boxY1); // (x, y, w, h)
         
         // Draw each joint dot that has been confirmed by accessing the JSONArray dataset
         for (int i = 0; i < dataset.size(); i++) {
            JSONObject curr = (JSONObject) dataset.get(i);
            String currJointName = (String) curr.get("name");
            int x = Integer.parseInt("" + curr.get("x-coordinate"));
            int y = Integer.parseInt("" + curr.get("y-coordinate"));
            g.setColor(jointDotColors.get(currJointName)); // Get appropriate color
            g.fillOval(x + xOffset - 4, y + yOffset - 4, 8, 8);
         }
         
         // If the confirm joint button was not clicked (the mouse button was clicked without confirming)
         if (mouseJointClicked) {
            // Draw an extra dot with the color of the selected joint where the mouse is clicked
            g.setColor(jointDotColors.get("" + joint.getSelectedItem()));
            g.fillOval(mouseX - 4, mouseY - 4, 8, 8);
            mouseJointClicked = false;
         }
      }
   }
   
   // Nested class used to display items within the joint drop-down list with their appropriate
   // dot color as its text color
   class ComboBoxRenderer extends JPanel implements ListCellRenderer {

      private static final long serialVersionUID = -1L;
      private java.util.List<Color> colors; // All of the colors in the appropriate order
      private java.util.List<String> strings; // All of the text items to be colored in the appropriate order
   
      JPanel textPanel;
      JLabel text;
      
      // Creates a new renderer for displaying colored text within a given drop-down list
      // Parameters:
      //    - JComboBox combo: the given drop-down list to be colored
      public ComboBoxRenderer(JComboBox combo) {
         textPanel = new JPanel();
         textPanel.add(this);
         text = new JLabel();
         text.setOpaque(true);
         text.setFont(combo.getFont());
         textPanel.add(text);
      }
   
      // Store the given list of colors
      public void setColors(java.util.List<Color> col)
      {
         colors = col;
      }
      
      // Store the given list of text items to be colored
      public void setStrings(java.util.List<String> str)
      {
         strings = str;
      }
      
      // Gets the list of colors being used
      public java.util.List<Color> getColors()
      {
         return colors;
      }
      
      // Gets the list of text items being used
      public java.util.List<String> getStrings()
      {
         return strings;
      }
      
      // Overriden method that displays the appropriate text items with colors from the list
      // of colors.
      // Parameters:
      //    - JList list: the list to be colored
      //    - Object value: the text at the given list index position
      //    - int index: the index position of the item in the list to be rendered
      //    - boolean isSelected: true if the above specified cell is selected
      //    - boolean cellHasFocus: true if the above specified cell has focus (mouse is hovering)
      @Override
      public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

         if (isSelected) // Special case if a cell is selected
         {
            setBackground(list.getSelectionBackground());
         }
         else
         {
            setBackground(Color.WHITE);
         }

         if (colors.size() != strings.size()) // Handle issue when the number of colors and text items are not equal
         {
            System.out.println("colors.length does not equal strings.length");
            return this;
         }
         else if (colors == null) // Make sure a list of colors was provided
         {
            System.out.println("use setColors first.");
            return this;
         }
         else if (strings == null) // Make sure a list of text items was provided
         {
            System.out.println("use setStrings first.");
            return this;
         }
         
         // Set normal background
         text.setBackground(getBackground());
         
         // Set appropriate text color and return colored text cell
         text.setText(value.toString());
         if (index>-1) {
            text.setForeground(colors.get(index));
         }
         return text;
      }
   }
}
