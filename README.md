# Image Labeling App
This application allows users to label object-based images with annotations such as joints, bounding box, and specific tags.  
  
## Running the Application from Terminal  
Click the clone button, copy the provided URL.  
Go to terminal, go to desired local repository cloning location, and paste/run `$ git clone <URL>`.  
Enter your credentials if prompted.  
Then enter the repository:  
    `$ cd image-labeling-app`  
Compile the application: (You may safely ignore any resulting notices)  
    `$ javac -cp ".:./json-simple-1.1.jar" LabelImage.java`  
Run the application:  
    `$ java -cp ".:./json-simple-1.1.jar" LabelImage`  
Note: `: above is ; for Windows`

## Supporting Files
  - `JointNames.txt`: Contains all of the joint names to be used as options in labeling.  
  - `JointDotColors.txt`: Contains all of the rgb colors for visually displaying the joints. Each line should represent a single color with three numbers representing rgb-values, in that order.  
  - `TagNames.txt`: Contains all of the specific tag descriptions to be used when labeling.  
  - `SampleDogJointsKey.png`: Image used in application as a reference for joint-labeling. Please do not modify.  
  - `SampleDogLabelFile.png`: Image used in application as an example output .label file for user's reference. Please do not modify.  
Note: Please do not modify the above file names.  
Note: The number of joints in "JointNames.txt" should equal the number of colors in "JointDotColors.txt" in order to avoid unexpected results.  
  
## Output
All labeled joint, bounding box, and tag data is outputted in JSON format to a corresponding .label file for each labeled image with name "<image-file-name>" + ".label".  
For instance, labeled data from "image.jpeg" would be outputted to a corresponding "image.jpeg.label" file. These .label files will be created in the user's local drive  
under the same directory as the respective image file.  
  
## Further notes
User can choose individual image files or entire directories with image files from local drive.  
Please refer to comments in LabelImage.java for further information
