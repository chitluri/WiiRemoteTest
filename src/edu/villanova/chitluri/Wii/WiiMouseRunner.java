package edu.villanova.chitluri.Wii;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;

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


public class WiiMouseRunner extends WiiRemoteAdapter
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
  
  
  
  /***************************
   * Constructor
   ***************************/
  public WiiMouseRunner(WiiRemote remote) throws IOException
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
    System.out.println("Disconnected the remote... Please re-connect");
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
				int tempX = X;
				int tempY = Y;
				if(!(Math.abs(lastX-X) < 5 && Math.abs(lastY - Y) < 5)){
					if(countHighsAndLows() == 0){
						//System.out.println("Move");
					}
					else{
						//System.out.println("Don't Move");
						//int values[] = findMedianXY(XValues, YValues);
						//tempX = values[0];
						//tempY = values[1];
						tempX = minHeapX.peek();
						tempY = minHeapY.peek();
					}
					robot.mouseMove(tempX, tempY);
					//lastTime = curTime;
					lastX = tempX;
					lastY = tempY;
				}
				
			  }
		  }
		  addXtoHeap(X);
		  addYtoHeap(Y);
		  addXtoList(X);
		  addYtoList(Y);
	} catch (AWTException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
  }
  
  public void addXtoList(int X){
	  if(XValues.size() == 100){
		  XValues.remove(0);
		  XValues.add(99, X);
		  if(maxHeapX.size() == 0 && XValues.get(0) > 0){
			  buildMaxMinHeapsX();
		  }
	  }
	  else{
		  XValues.add(X);
	  }
  }
  
  public void addYtoList(int Y){
	  if(YValues.size() == 100){
		  YValues.remove(0);
		  YValues.add(99, Y);
		  if(maxHeapY.size() == 0 && YValues.get(0) > 0){
			  buildMaxMinHeapsY();
		  }
	  }
	  else{
		  YValues.add(Y);
	  }
  }
  
  public void addXtoHeap(int X){
	  if(minHeapX.size() == 50 && maxHeapX.size() == 50){
		  int elemToRemove = XValues.get(0);
		  if(maxHeapX.contains(elemToRemove)){
			  maxHeapX.remove(elemToRemove);
		  }
		  else if(minHeapX.contains(elemToRemove)){
			  minHeapX.remove(elemToRemove);
		  }
		  else{
			  System.out.println("Fatal Error: Cannot remove element from X Heap");
		  }
		  
		  if(X < maxHeapX.peek()){
			  if(maxHeapX.size() == 49){
				  maxHeapX.add(X);
			  }
			  else if(minHeapX.size() == 49){
				  minHeapX.add(maxHeapX.poll());
				  maxHeapX.add(X);
			  }
		  }
		  else{
			  if(minHeapX.size() == 49){
				  minHeapX.add(X);
			  }
			  else if(maxHeapX.size() == 49){
				  maxHeapX.add(minHeapX.poll());
				  minHeapX.add(X);
			  }
		  }
	  }
  }
  
  public void addYtoHeap(int Y){
	  if(minHeapY.size() == 50 && maxHeapY.size() == 50){
		  int elemToRemove = YValues.get(0);
		  if(maxHeapY.contains(elemToRemove)){
			  maxHeapY.remove(elemToRemove);
		  }
		  else if(minHeapY.contains(elemToRemove)){
			  minHeapY.remove(elemToRemove);
		  }
		  else{
			  System.out.println("Fatal Error: Cannot remove element from Y Heap");
		  }
		  
		  if(Y < maxHeapY.peek()){
			  if(maxHeapY.size() == 49){
				  maxHeapY.add(Y);
			  }
			  else if(minHeapY.size() == 49){
				  minHeapY.add(maxHeapY.poll());
				  maxHeapY.add(Y);
			  }
		  }
		  else{
			  if(minHeapY.size() == 49){
				  minHeapY.add(Y);
			  }
			  else if(maxHeapY.size() == 49){
				  maxHeapY.add(minHeapY.poll());
				  minHeapY.add(Y);
			  }
		  }
	  }
  }
  
  public void buildMaxMinHeapsX(){
	  for(int i=0; i<50; i++){
		  maxHeapX.add(XValues.get(i));
	  }
	  for(int i=50; i<100; i++){
		  if(XValues.get(i) < maxHeapX.peek()){
			  int temp = maxHeapX.poll();
			  maxHeapX.add(XValues.get(i));
			  minHeapX.add(temp);
		  }
		  else{
			  minHeapX.add(XValues.get(i));
		  }
	  }
  }
  
  public void buildMaxMinHeapsY(){
	  for(int i=0; i<50; i++){
		  maxHeapY.add(YValues.get(i));
	  }
	  for(int i=50; i<100; i++){
		  if(YValues.get(i) < maxHeapY.peek()){
			  int temp = maxHeapY.poll();
			  maxHeapY.add(YValues.get(i));
			  minHeapY.add(temp);
		  }
		  else{
			  minHeapY.add(YValues.get(i));
		  }
	  }
  }
  
  public int[] findMedianXY(ArrayList<Integer> XValues, ArrayList<Integer> YValues){
	  ArrayList<Integer> newXValues = new ArrayList<Integer>(XValues);
	  ArrayList<Integer> newYValues = new ArrayList<Integer>(YValues);
	  Collections.sort(newXValues);
	  Collections.sort(newYValues);
	  int X = newXValues.get(newXValues.size()/2);
	  int Y = newYValues.get(newYValues.size()/2);
	  return new int[]{X, Y};
  }
  
  public static void main(String args[]) throws IllegalStateException, IOException
  {
 

    System.setProperty(BlueCoveConfigProperties.PROPERTY_JSR_82_PSM_MINIMUM_OFF, "true");
    //basic console logging options...
    WiiRemoteJ.setConsoleLoggingAll();
    //WiiRemoteJ.setConsoleLoggingOff();
    
    try
    {
      // Find a remote and make a connection       
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
      
      remote.addWiiRemoteListener(new WiiMouseRunner(remote));
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
	  
	  if(XValues.get(0) < XValues.get(1)){
		  isIncreasing = true;
		  height += 2;
	  }
	  else{
		  isDecreasing = true;
		  depth += 2;
	  }
	  
	  for(int i=2; i < 100; i++){
		  //valuesSum += XArray[i];
		  if(XValues.get(i) == 0){
			  break;
		  }
		  if(isIncreasing){
			  if(XValues.get(i-1) > XValues.get(i)){
				  if(height >= 5){
					  highs++;
				  }
				  height = 0;
				  isIncreasing = false;
				  isDecreasing = true;
				  depth = 2;
			  }
			  height++;
		  }
		  else if(isDecreasing){
			  if(XValues.get(i-1) <= XValues.get(i)){
				  if(depth >= 5){
					  lows++;
				  }
				  depth = 0;
				  isIncreasing = true;
				  isDecreasing = false;
				  height = 2;
			  }
			  depth++;
		  }
	  }
	  //System.out.println("Highs: "+highs+" Lows: "+lows);
	  //System.out.println("Sum of all values: "+valuesSum);
	  
	  
	  if(highs >= 4 && lows >= 4){
		return -1;
	  }
	  else{
		  return 0;
	  }
	  
  }
}