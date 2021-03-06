import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.applet.*;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import javax.swing.border.*;
import javax.swing.*;

class Lines implements Serializable {

	ArrayList<Line> lines = new ArrayList<Line>();

	public void setArray(ArrayList<Line> lines){
		this.lines = lines;
	}

	public ArrayList<Line> getArray(){
		return lines;
	}
}

class Line implements Serializable{
	public Line(int sX, int sY, int eX, int eY, Color c){
		setStartX(sX);
		setStartY(sY);
		setEndX(eX);
		setEndY(eY);
		setColor(c);
	}
	private int startX;
	private int startY;
	private int endX;
	private int endY;
	private Color color;

	public void setStartX(int n){
		startX = n;
	}
	public void setStartY(int n){
		startY = n;
	}
	public void setEndX(int n){
		endX = n;
	}
	public void setEndY(int n){
		endY = n;
	}
	public void setColor(Color c){
		color = c;
	}
	public int getStartX(){return startX;}
	public int getStartY(){return startY;}
	public int getEndX(){return endX;}
	public int getEndY(){return endY;}
	public Color getColor(){return color;}
}


public class Client extends Frame implements MouseListener, MouseMotionListener, ActionListener{
	static boolean debug = false;
	Panel messenger;
	Button send;
	TextField tf;
	TextArea ta;
	TextArea userListArea;
	String name;
	Panel content;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	int lastX;
	int lastY;
	int currX;
	int currY;
	Color color;
	JButton bR;
	JButton bG;
	JButton bB;
	JButton bBlack;
	JButton clear;
	JButton load=new JButton("Load");
   JButton save;	
Panel colors;
	boolean isBackspace = false;
	boolean didEnter = false;
	boolean archiveRecieved = false;
	boolean firstRun = true;
	boolean clearScreen = false;
	boolean clearScreenFromServer = false;
	boolean addLineFromServer = false;
	boolean dragged = false;
	ArrayList<Line> lines = new ArrayList<Line>();

	public Client(String name, String ip){
		this.name = name;
		setTitle("Whiteboard ChatApp - " +"Welcome : "+name+"  !!");
		setSize(700,500);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we){
				log("Client Shutting Down");
				System.exit(0);
			}
		});

		//Whiteboard Init
		setLayout(new BorderLayout());

		colors = new Panel();
		colors.setLayout(new GridLayout(7,1));

		bR = new JButton("Red");
                                       Font myFont = new Font("Courier",Font.BOLD,20);
                                       bR.setFont(myFont);
                                       bR.setForeground(Color.RED);
                                       bR.setBackground(Color.WHITE);
                                       bR.setBorder(new LineBorder(Color.RED, 5));
		bR.addActionListener(this);

		bG = new JButton("Green");
                                       bG.setFont(myFont);
                                       bG.setForeground(Color.GREEN);
                                       bG.setBackground(Color.WHITE);
                                       bG.setBorder(new LineBorder(Color.GREEN, 5));
		bG.addActionListener(this);

		bB = new JButton("Blue");
                                       bB.setFont(myFont);
                                       bB.setForeground(Color.BLUE);
                                       bB.setBackground(Color.WHITE);
                                       bB.setBorder(new LineBorder(Color.BLUE,5));
		bB.addActionListener(this);

		bBlack = new JButton("Black");
                                       bBlack.setFont(myFont);
                                       bBlack.setForeground(Color.BLACK);
                                       bBlack.setBackground(Color.WHITE);
                                       bBlack.setBorder(new LineBorder(Color.BLACK, 5));
		bBlack.addActionListener(this);

		clear = new JButton("Clear");
		clear.addActionListener(this);
                                       
                                       save=new JButton("Save");
                                       save.addActionListener(this);
		

		load.addActionListener(this);
		tf.addActionListener(this);


addMouseMotionListener(this);
		addMouseListener(this);

		colors.add(bR);
		colors.add(bG);
		colors.add(bB);
		colors.add(bBlack);
		colors.add(clear);
                                       colors.add(save);
		colors.add(load);
		
		//Chat Init
		messenger = new Panel();
		messenger.setLayout(new BorderLayout());
		send = new Button("Send");
		send.addActionListener(this);
		messenger.add(send, BorderLayout.SOUTH);
		tf = new TextField();
		messenger.add(tf,BorderLayout.NORTH);
		ta = new TextArea(1,20);
		ta.setEditable(false);
		messenger.add(ta, BorderLayout.CENTER);
		userListArea = new TextArea(1,10);
		userListArea.setEditable(false);
		messenger.add(userListArea, BorderLayout.EAST);
		
		
		//Add all to window
		add(messenger, BorderLayout.EAST);
		add(colors, BorderLayout.WEST);

		for(int i = 1; i <= 4; i++){
			try{
				//Initialize Connection to Server
				log("Attempt Connection");
				Socket s = new Socket(ip, 2146);
				i = 1;
				log("Connection Made");
			
				log("Initialize Streams.");
			
				log("--Initializing Output Stream.");
				oos = new ObjectOutputStream(s.getOutputStream());
				log("--Initialized Output Stream.");
			
				log("--Initializing Input Stream.");
				ois = new ObjectInputStream(s.getInputStream());
				log("--Initialized Input Stream.");
			
				log("Initialized Streams");
				
				//Receive and draw initial line archive packet
				log("Wait for Archive Receipt");
				ChildDataObject  archive = (ChildDataObject )ois.readObject();
			
				log("Archive Received");
				lines = archive.getLines();

				//Send Name
				ChildDataObject  initpacket = new ChildDataObject (name);
				sender(initpacket);
				
				log("Paint and show window");
				repaint();
				setVisible(true);
				
				//Listen for incoming server comands ad infinum
				for(;;){
					log("Wait for server packet...");
					ChildDataObject  packet = (ChildDataObject )ois.readObject();
					parsePacket(packet);
					log("Packet received");
				}

			} catch(Exception e){
				log("Connection to server LOST");
				setVisible(false);
				//e.printStackTrace(); Can be enabled for diagnostic purposes
				if(i==4)
					continue;
				log("Waiting 5 seconds before attempting to reconnect");
				try{Thread.sleep(5000);
				} catch(InterruptedException ie){
					log("Thread Sleep Interrupted");
				}
				log("Attempting to reconnect to server...Attempt " + i);
				
			}
		}
		log("Client Shutting Down");
		System.exit(0);
	}

	public void parsePacket(ChildDataObject  packet){
		log("Parsing Packet");

		switch(packet.getPayload()) {
			case 0: log("Bad Packet Received");
					break;

			case 1: log("Clear Packet Received");
					clearScreenFromServer = true;
					repaint();
					break;

			case 2: log("Full redraw Packet Received");
					clearScreenFromServer = true;
					repaint();
					archiveRecieved = true;
					lines = packet.getLines(); 
					repaint();
					break;

			case 3: log("New line Packet Received");
					lines.add(packet.getLine());
					addLineFromServer = true;
					repaint();
					break;
			case 4: log("New message Received");
					ta.append(packet.getMessage());
					break;
			case 5: log("User List Received");
					updateUserList(packet);
					break;
			default: break;
		}
	}

	public void sender(ChildDataObject  packet){
		log("Preparing to send packet.");
		try {
			log("Sending Packet");
			oos.writeObject(packet);
			log("Packet Sent");
		} catch(IOException e){log("Packet send fail.");}
	}

	public void updateUserList(ChildDataObject  packet){
		String[] userList = packet.getUserList();
		
		userListArea.setText("Users Online");
		
		for(int i = 0 ; i < userList.length ; i++){
			userListArea.setText(userListArea.getText() + "\n" + userList[i]);
		}
	}
	
	public void paint(Graphics g){
		log("Paint Called");
		try {
		if(clearScreen || clearScreenFromServer){
			log("Preparing to clear screen.");
			lines = new ArrayList<Line>();
			Dimension d = getSize();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, d.width, d.height);
			if(clearScreen){
				sender(new ChildDataObject (true));
				log("Clear packet sent to server.");
			}
			clearScreen = false;
			clearScreenFromServer = false;
			log("Screen cleared.");
			return;
		}
		if(!dragged || firstRun /*|| archiveRecieved*/){
			firstRun = false;
			archiveRecieved = false;
			for (Line l : lines){
				g.setColor(l.getColor());
				g.drawLine(l.getStartX(), l.getStartY(), l.getEndX(), l.getEndY());
			}
		}

		g.setColor(color);
		g.drawLine(lastX, lastY, currX, currY);
		Line line = new Line(lastX, lastY, currX, currY, color);
		lines.add(line);
		if(!addLineFromServer)
			sender(new ChildDataObject (line));
		addLineFromServer = false;
		record(currX, currY);
		} catch (Exception e){
			log("CONCURRENT PAINT EXCEPTION");
		}
	}
public void save()
                    {
	try {
            String str = ta.getText();
            File f = new File("ChatHistory.txt");

            FileWriter fw = new FileWriter(f);
            fw.write(str);
            fw.close();
   
        }
 catch (IOException iox) {
            iox.printStackTrace();
        }


// Drawing SAve


try {
BufferedImage saving = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = saving.createGraphics();
        paint(graphics);
        File map = new File("storeDraw.jpg");
        ImageIO.write(saving, "jpg", map);
     } catch(IOException exc) {
        System.out.println("problem saving");
    }

  JOptionPane.showMessageDialog(this, " Chat and Drawing Saved !! ");


 }

public void load()
{
try{

ChildDataObject  archive = (ChildDataObject )ois.readObject();
lines = archive.getLines();
repaint();
}
catch(Exception e) 
{}
}



	public void update(Graphics g){
		paint(g);
	}
	public void record(int x, int y){
		lastX = x;
		lastY = y;
	}
	public void mouseEntered(MouseEvent e){
		record(e.getX(), e.getY());
	}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e){
		dragged = !dragged;
		record(e.getX(), e.getY());
	}
	public void mouseReleased(MouseEvent e){
		dragged = !dragged;

	}
	public void mouseClicked(MouseEvent e){}
	public void mouseMoved(MouseEvent e){}
	public void mouseDragged(MouseEvent e){
		currX = e.getX();
		currY = e.getY();
		repaint();
	}
	public void actionPerformed(ActionEvent ae){
		//Whiteboard
		if(ae.getSource() == bR){
			color = Color.RED;
			return;
		}
		if(ae.getSource() == bG){
			color = Color.GREEN;
			return;
		}
		if(ae.getSource() == bB){
			color = Color.BLUE;
			return;
		}
		if(ae.getSource() == bBlack){
			color = Color.BLACK;
			return;
		}
		if(ae.getSource() == clear){
			clearScreen = true;
			repaint();
			return;
		}
		
if((ae.getSource() == load)){
			load();
			return;
		} 

		//Chat
		if((ae.getSource() == send)){
			sendMessage();
		} else {
			sendMessage();
		}
  if(ae.getSource() == save){
			save();
                                                         // clearScreen = true;
			//repaint();
			return;
		}
                                    

	}
	
	public void sendMessage(){
		if(!(tf.getText().equals(""))){
			log("Send Pressed");
			sender(new ChildDataObject (name +  ": " + tf.getText() + "\n"));
			ta.append(name +  ": " + tf.getText() + "\n");
			tf.setText("");
		}
	}
		
	public static void log(String message){
		if(debug){
			String source = "LOG: ";
			System.out.println(source + message);
		}
	}

	public static void main(String[] args){
		String ip = "localhost";
		String name = "Guest";
		
		if(args.length > 0){
			if(args.length == 1){
				name = args[0];
			} else {
				name = args[0];
				ip = args[1];
			}
		}		
		log("Searching for server at: " + ip);
		log("To specify a server other than localhost use the formula: <java Draw 'name' '192.168.0.1'>");
		Client client = new Client(name, ip);
	}
}
