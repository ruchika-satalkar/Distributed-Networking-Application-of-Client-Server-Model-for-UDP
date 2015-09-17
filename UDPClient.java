package udpClient;
import java.net.*;
import java.io.InterruptedIOException; 
import java.util.Arrays;
import java.util.Scanner;

public class UdpClient {
	public static final String SYMBOL = "[CR+LF]";
	public static final String PROTOCOL = "ENTS/1.0";
	public static final int C = 7919;
	public static final int D = 65536;
	public static void main(String[] args) throws Exception {
		
			final int SERVER_PORT_NUM = 9984;
			final int RX_MESSAGE_SIZE = 8000;
			int timeoutValMs= 1000; // in milliseconds
			int timerFlag=0;
			String sHostName = "dell-PC"; // local host name
			InetAddress serverIp = InetAddress.getByName(sHostName);
			DatagramSocket clientSocket = new DatagramSocket();
			Scanner input = new Scanner(System.in);
			while(true) 
			{
				System.out.println("\nWhich of the following files would you like to receive from the server?\n1. directors_message.txt\n2. program_overview.txt\n3. scholarly_paper.txt\n4. Exit\n");
				int choice = input.nextInt();
				int resend=1;
				if(choice>3) // discarding the invalid input
				{
					clientSocket.close();
					input.close();
					System.out.println("No file requested. Client socket closed!");
					System.exit(0);
				}
				else
			    {
					String reqMessage = AssembleReqMessage(choice); // Request file according to user input
					int intCheck= integrityCheck(reqMessage);    // Calculating the integrity check value of the assembled request
					String checksum = Integer.toString(intCheck);
					reqMessage=reqMessage+checksum+SYMBOL;  // the final string to be sent to the server
					System.out.println("Complete request message with integrity check value:\n"+reqMessage);
					byte[] reqBytes=reqMessage.getBytes();
					DatagramPacket reqPacket = new DatagramPacket(reqBytes, reqBytes.length, serverIp, SERVER_PORT_NUM); 
					byte[] responseBytes = new byte[RX_MESSAGE_SIZE]; 
					DatagramPacket responsePacket = new DatagramPacket(responseBytes, RX_MESSAGE_SIZE);
					
					for(timerFlag=0; timerFlag<4&&resend==1; timerFlag++) 
					{
						clientSocket.setSoTimeout(timeoutValMs); 
						clientSocket.send(reqPacket); 
						System.out.print("\nRequest message sent to the server."); 
						try
						 { 
							System.out.println("\nWaiting for the server's response..."); 
							clientSocket.receive(responsePacket); // the timeout timer starts ticking here
							timeoutValMs=1000;
							int respLength = responsePacket.getLength();
							String respText = new String(responseBytes, 0, respLength); // truncate the string to recalculate the integrity check
							
							String originalReq= SepIntCheck(respText);
							int val =originalReq.length();
							int val1 =respText.length()-SYMBOL.length();
							String intExtract = respText.substring(val, val1);
							int intResCheck = Integer.parseInt(intExtract);			
							int intCheckVal= integrityCheck(originalReq);
							
							if(intCheckVal==intResCheck) // comparing the integrity check value supplied in the message and the value calculated
							{
								int responseCode= RespCode(respText); // To extract the response code from the response string
								switch(responseCode)
								{
									case 0:
										System.out.println("OK. The response has been received correctly according to the request.");
										resend=0;
										System.out.println("\nThe received response from the server is:\n "+respText);
										break;
									case 1:
										System.out.println("Integrity Check of the request message failed ! Would u like to resend the req? Yes/No");
										String s=input.nextLine();
										if(s=="Yes")
											timerFlag=0; // and resend the file as per user's choice
										else
										{
											resend=0; // do not resend
											break ;
										}
									case 2:
										System.out.println("Error: malformed request. The syntax of the request message is not correct.");
										resend=0; // do not resend
										break;
									case 3:
										System.out.println("Error: non-existent file. The file with the requested name does not exist.");
										resend=0;  // do not resend
										break;
									case 4:
										System.out.println("Error: wrong protocol version. The version number in the request is different from 1.0.");
										resend=0;   // do not resend
										break;
								}		
							 }
							else 
							{
								System.out.println("\nIntegrity check of the request failed at the server side! Resend request.");
								timerFlag=0; // and resend the file 
							
							}
						} 
						catch(InterruptedIOException e) 
						{ 
							// timeout - timer expired before receiving the response from the server
							System.out.println("\nTime out for the "+(timerFlag+1)+". time!"); 
							timeoutValMs*=2;
							if(timerFlag==3)
							 {
								System.out.println("Error! Communication failed!");
								timeoutValMs=1000;
								break;
								// resend the file
							 }				
						} 
					} 
			    }
			}
	}
	
	public static String AssembleReqMessage(int choice){
		String requestMsg;
		String file;
		switch(choice)
		{
		case 1:
			file="directors_message.txt";
			break;
		case 2:
			file="program_overview.txt";
			break;
		case 3:
			file="scholarly_paper.txt";
			break;
		default:
			file="";
			break;	
		}		
		requestMsg=PROTOCOL+" Request"+SYMBOL+file+SYMBOL;
		System.out.println("Assembled request message: "+requestMsg);
		return requestMsg;
	}
		
	public static int integrityCheck(String reqMsg){
		byte[] req= reqMsg.getBytes();
		byte[] reqCopy = Arrays.copyOf(req, req.length+1);
		if(req.length%2!=0)
		{
			reqCopy[req.length]=0;	
		}
		else
		{
			reqCopy=req;
		}
		int S=0;
		for(int j=0; j<reqCopy.length; j+=2)
		{
			byte[] word= new byte[2];	//to store 16 bit words
			word[0]=reqCopy[j];
			word[1]=reqCopy[j+1];
			int index;
			int wordInt=(word[0]<<8)+word[1];	//forming a word of two consecutive bytes
			index=wordInt^S;		
			S=(C*index)%D;	
		}
		return S;
	}
	public static String SepIntCheck(String resText)
	{
		int k=0;
		int o=0;
		int strlen=SYMBOL.length();
		int n=resText.indexOf(SYMBOL);
		k=resText.indexOf( SYMBOL, n+1 );
		o=resText.indexOf( SYMBOL, k+1 );
		String resText1 = resText.substring(k+strlen,o);  
		int i = Integer.parseInt(resText1);
		int h=o+strlen+i;
		String resText2 = resText.substring(0,h+strlen);
		return resText2;
	}
		
	public static int RespCode(String respText){
		int k1=0;
		int strlen1=SYMBOL.length();
		int n1=respText.indexOf(SYMBOL);
		k1=respText.indexOf( SYMBOL, n1+1 );
		String respText2 = respText.substring(n1+SYMBOL.length(), k1);
		int r = Integer.parseInt(respText2);
		return r;
	}
			
}
