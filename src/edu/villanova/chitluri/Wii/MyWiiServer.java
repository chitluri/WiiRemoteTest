package edu.villanova.chitluri.Wii;


import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Robot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import wiiremotej.ButtonMap;
import wiiremotej.ButtonMouseMap;
import wiiremotej.IRLight;
import wiiremotej.WiiRemote;
import wiiremotej.WiiRemoteExtension;
import wiiremotej.WiiRemoteJ;
import wiiremotej.event.WRAccelerationEvent;
import wiiremotej.event.WRButtonEvent;
import wiiremotej.event.WRExtensionEvent;
import wiiremotej.event.WRIREvent;
import wiiremotej.event.WRNunchukExtensionEvent;
import wiiremotej.event.WRStatusEvent;
import wiiremotej.event.WiiRemoteAdapter;

import com.intel.bluetooth.BlueCoveConfigProperties;

//********************************************************
//NOTE
//********************************************************

// Also, please note that all code surrounded by these types of comments 
// are modification's to Michael Diamond's original 1/05/07 WRLImpl.java code
// which I found at:
// http://www.world-of-cha0s.hostrocket.com/WiiRemoteJ/WiiRemoteJ%20v1.6.zip.gz
// an original copy is included in the  source code.

//////////////////////////////////////////////////////////

/**
 * Implements WiiRemoteListener and acts as a general test class. Note that you can ignore the main method pretty much, as it mostly has to do with the graphs and GUIs.
 * At the very end though, there's an example of how to connect to a remote and how to prebuffer audio files.
 * 
 * @ModifyingAuthor by Jonathan Leung
 * @Version 6/1/10
 * 
 * @OriginalAuthor Michael Diamond
 * @OriginalVersion Version 1/05/07
 * 
 */

public class MyWiiServer extends WiiRemoteAdapter
{

  private static boolean printMessage = true; //Print every time the Wiimote sends an update
  public static IntermediaryObservervable observable; // Used to communicate to WiiServerThreads
  private static int count = 0;
    
  private WiiRemote remote;
  
  private static int lastX = 0;
  private static int lastY = 0;
  private static JPanel graph;
  
  private static ArrayList<Integer> XValues = new ArrayList<Integer>();
  private static ArrayList<Integer> YValues = new ArrayList<Integer>();
  private static PriorityQueue<Integer> minHeapX = new PriorityQueue<Integer>(50);
  private static PriorityQueue<Integer> maxHeapX = new PriorityQueue<Integer>(50, Collections.reverseOrder());
  private static PriorityQueue<Integer> minHeapY = new PriorityQueue<Integer>(50);
  private static PriorityQueue<Integer> maxHeapY = new PriorityQueue<Integer>(50, Collections.reverseOrder());
  
  private static ArrayList<Integer> displacementX = new ArrayList<Integer>();
  private static ArrayList<Integer> displacementY = new ArrayList<Integer>();
  
  private static int problemWidth;
  private static int problemHeight;
  private static boolean isMeasurementDone = false;
  
  
  /***************************
   * Constructor
   ***************************/
  public MyWiiServer(WiiRemote remote) throws IOException
  {
    this.remote = remote;
    
    // Start the WiiServer in a new thread
    (new Thread(new WiiServer())).start();  
    
    // Create an observable object that every WiiServerThread can observe
    observable = new IntermediaryObservervable();
    
    System.out.println("Started Server");
  } 

  
  /*******************************************************************************
   * IntermediaryObservable
   * 
   * Please read the section on Observers and Observables README for 
   * a more detailed explanation for how and why they are needed in this project.
   * 
   * Essentially, IntermediaryObservable passes messages from this WiiServerRunner.java file
   * to all WiiServerThreads that are currently running which pass it to their 
   * Receiver. It does this by using Java's Observable class.
   * 
   * These String messages are formed by concatenating WiiMote and Nunchuck 
   * accelerometer data into one string message and is sent every time
   * accelerationInputReceived() is called.
   * 
   * Optional Reading: Another attempted implementation was to not concatenate 
   * all the data into one String, messageToBeObserved,  and send it over as 
   * soon as each piece of data is received. However, because 
   * accelerationInputReceived() and extensionInputReceived() were both called
   * at very frequent rates,  many packets were lost with this  implementation.
   ******************************************************************************/
  class IntermediaryObservervable extends Observable {
    // Tells any observers that what they are observing has changed.
    protected void setChanged() {
      super.setChanged();
    }
  }
  
  
  /*****************************************************************************
   * newObserver()
   * 
   * Since each WiiServerThread is an observer of IntermediaryObservable,
   * newObserver() is called by WiiServer every time a thread is added
   * (meaning that a new Receiver has been added)
   *****************************************************************************/   
  public static void newObserver(Observer observer) {
    observable.addObserver(observer);
  }
 

  /*****************************************************************************
   * sendToBeObservedByWiiServerThread()
   * 
   * Sends the message that contains accelerometer and button data to all
   * WiiServerThread Observers.
   * 
   * Note that each message that is sent has a unique id, the count.
   * The count is an indicator of how many messages have been sent before it
   * and is used on the receiving end to determine if WiiServerRunner.java sent any packets
   * that were not received. This can be determined if on the receiving end,
   * the count jumps from say 1001 to 1003. You know you lost message 1002.
   * 
   * From testing locally, there is an extremely minimal packet loss 
   * (1 in thousands of packets) which doesn't really make a difference.
   * 
   *****************************************************************************/   
  public void sendToBeObservedByWiiServerThread(String message) {
    
    //Tell WiiServerThread observers that what they are observing has changed
    observable.setChanged(); 
    
    //Construct the message to be observed by the WiiServerThread
    message += "count=" + Integer.toString(count) + ", "; //add the message count number
    
    //Notify the observers with the updated instance of what they are observing
    observable.notifyObservers(message);
    count++;
    
    if (printMessage) { //SETTING
      System.out.println(message);
    }
  }
  
  /*****************************************************************************
   * getTime()
   * 
   * Returns the current system time in milliseconds as a string.
   *****************************************************************************/   
  public String getTime() {
    return Long.toString(System.currentTimeMillis());
  }
  
  
  public void accelerationInputReceived(WRAccelerationEvent evt)
  {
    //Do Nothing. Not relevant for this project.
  }
  
    
  public void extensionInputReceived(WRExtensionEvent evt)
  {
	  //Do Nothing. Not relevant for this project.
  }
  
  public void buttonInputReceived(WRButtonEvent evt)
  { 
	  //Do Nothing. Not relevant for this project.
  }
  
  public void extensionConnected(WiiRemoteExtension extension)
  {
    System.out.println("Extension connected!");
    try
    {
      remote.setExtensionEnabled(true);
    }catch(Exception e){e.printStackTrace();}
  }
  
  public void extensionPartiallyInserted()
  {
    System.out.println("Extension partially inserted. Push it in more next time!");
  }
  
  public void extensionUnknown()
  {
    System.out.println("Extension unknown. Did you try to plug in a toaster or something?");
  }
  
  public void extensionDisconnected(WiiRemoteExtension extension)
  {
    System.out.println("Extension disconnected. Why'd you unplug it, eh?");
  }
  
  public void disconnected()
  {
    System.out.println("Remote disconnected... Please Wii again.");
    System.exit(0);
  }
  
  public void statusReported(WRStatusEvent evt)
  {
    System.out.println("Battery level: " + (double)evt.getBatteryLevel()/2+ "%");
    System.out.println("Continuous: " + evt.isContinuousEnabled());
    System.out.println("Remote continuous: " + remote.isContinuousEnabled());
  }
  
  public void IRInputReceived(WRIREvent evt)
  {
	  try {
		  Robot robot = new Robot();
		  int X = 0;
		  int Y = 0;
		  for (IRLight light : evt.getIRLights()) {
			  if (light != null){
				X = (int) Math.round((1.0D - light.getX()) * 1440);
				Y = (int) Math.round(light.getY() * 900);
				robot.mouseMove(X, Y);
				
			  }
		  }
		  addXtoList(X);
		  addYtoList(Y);
		  System.out.println("Calling");
		  countHighsAndLows();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
  }
  
  public void addXtoList(int X){
	  if(XValues.size() == 200){
		  XValues.remove(0);
		  XValues.add(199, X);
	  }
	  else{
		  XValues.add(X);
	  }
  }
  
  public void addYtoList(int Y){
	  if(YValues.size() == 200){
		  YValues.remove(0);
		  YValues.add(199, Y);
	  }
	  else{
		  YValues.add(Y);
	  }
  }
  
  
  
  public static void main(String args[]) throws IllegalStateException, IOException
  {
 

    System.setProperty(BlueCoveConfigProperties.PROPERTY_JSR_82_PSM_MINIMUM_OFF, "true");
    //basic console logging options...
    WiiRemoteJ.setConsoleLoggingAll();
    //WiiRemoteJ.setConsoleLoggingOff();
    
    try
    {
      
    	
        //graph.repaint();
    	
      //********************************************************
      // Find and connect to a Wii Remote
      //********************************************************        
      WiiRemote remote = null;
      
      while (remote == null) {
        try {
          remote = WiiRemoteJ.findRemote();
        }
        catch(Exception e) {
          remote = null;
          e.printStackTrace();
          System.out.println("Failed to connect remote. Trying again.");
        }
      }
      
      remote.addWiiRemoteListener(new MyWiiServer(remote));
      //remote.setSpeakerEnabled(true);
      remote.setIRSensorEnabled(true, WRIREvent.BASIC);
      remote.setLEDIlluminated(0, true);
      remote.enableContinuous();
    
      remote.getButtonMaps().add(new ButtonMap(WRButtonEvent.HOME, ButtonMap.NUNCHUK, WRNunchukExtensionEvent.C, new int[]{java.awt.event.KeyEvent.VK_CONTROL}, 
        java.awt.event.InputEvent.BUTTON1_MASK, 0, -1));
          
      remote.getButtonMaps().add(new ButtonMouseMap(WRButtonEvent.B, java.awt.event.InputEvent.BUTTON1_MASK));
      remote.getButtonMaps().add(new ButtonMouseMap(WRButtonEvent.A, java.awt.event.InputEvent.BUTTON3_MASK));
      
      final WiiRemote remoteF = remote;
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){remoteF.disconnect();}}));
    }
    catch(Exception e){e.printStackTrace();}
  }
  
  public int countHighsAndLows(){
	  int highs = 0;
	  int lows = 0;
	  int height = 0;
	  int depth = 0;
	  boolean isIncreasing = false;
	  boolean isDecreasing = false;
	  int minDisplacementX = -1;
	  int maxDisplacementX = -1;
	  int minDisplacementY = -1;
	  int maxDisplacementY = -1;
	  
	  if(XValues.get(0) < XValues.get(1)){
		  isIncreasing = true;
		  height += 2;
		  minDisplacementX = XValues.get(0);
	  }
	  else{
		  isDecreasing = true;
		  depth += 2;
		  maxDisplacementX = XValues.get(0);
		  maxDisplacementY = YValues.get(0);
	  }
	  
	  for(int i=2; i < 200; i++){
		  //valuesSum += XArray[i];
		  if(XValues.get(i) == 0){
			  break;
		  }
		  if(isIncreasing){
			  if(XValues.get(i-1) > XValues.get(i)){
				  if(height >= 5){
					  highs++;
					  maxDisplacementX = XValues.get(i-1);
					  displacementX.add(maxDisplacementX - minDisplacementX);
					  displacementY.add(maxDisplacementY - minDisplacementY);
				  }
				  height = 0;
				  isIncreasing = false;
				  isDecreasing = true;
				  depth = 2;
				  maxDisplacementX = XValues.get(i);
				  maxDisplacementY = YValues.get(i);
			  }
			  height++;
		  }
		  else if(isDecreasing){
			  if(XValues.get(i-1) <= XValues.get(i)){
				  if(depth >= 5){
					  lows++;
					  minDisplacementX = XValues.get(i-1);
					  minDisplacementY = YValues.get(i-1);
					  displacementX.add(maxDisplacementX - minDisplacementX);
					  displacementY.add(maxDisplacementY - minDisplacementY);
				  }
				  depth = 0;
				  isIncreasing = true;
				  isDecreasing = false;
				  height = 2;
				  minDisplacementX = XValues.get(i);
				  minDisplacementY = YValues.get(i);
			  }
			  depth++;
		  }
	  }
	  //System.out.println(XValues);
	  //System.out.println("Highs: "+highs+" Lows: "+lows);
	  //System.out.println("Sum of all values: "+valuesSum);
	  
	  
	  if(highs >= 13 && lows >= 13){
		if (!isMeasurementDone) {
			Collections.sort(displacementX);
			Collections.sort(displacementY);
			System.out.println(displacementX);
			System.out.println(displacementY);
			problemWidth = Math
					.abs(displacementX.get(displacementX.size() / 2));
			problemHeight = Math
					.abs(displacementY.get(displacementY.size() / 2));
			isMeasurementDone = true;
			JFrame graphFrame = new JFrame();
			graphFrame.setTitle("Accelerometer graph: Wii Remote");
			graphFrame.setSize(600, 600);
			graphFrame.setResizable(false);
			graph = new JPanel() {
				public void paintComponent(Graphics graphics) {
					//graphics.clearRect(240, 240, 500, 500);
					graphics.setColor(Color.red);
					graphics.fillOval(240, 240, problemWidth, problemHeight);
				}
			};
			graphFrame.add(graph);
			graphFrame.setVisible(true);
		}
		return -1;
	  }
	  else{
		  displacementX.clear();
		  displacementY.clear();
		  return 0;
	  }
	  
  }
}
