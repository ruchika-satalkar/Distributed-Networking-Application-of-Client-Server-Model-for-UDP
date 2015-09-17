package udpServer;
import java.net.*;
import java.io.*; 
import java.util.*;
public class UdpServer{
	
	public static final String SYMBOL = "[CR+LF]";
	public static final String PROTOCOL = "ENTS/1.0";
	public static final int C = 7919;
	public static final int D = 65536;
	public static void main(String[] args) throws Exception {
		
		final int SERVER_PORT_NUM = 9984;
		final int RX_MESSAGE_SIZE = 8000;
		byte[] receivedMessage = new byte[8000];
		DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT_NUM);
		DatagramPacket receivePacket = new DatagramPacket(receivedMessage,receivedMessage.length);
		while(true)
		{
			System.out.println("\nWaiting for a message from the client.");
			// receiving client's request
			serverSocket.receive(receivePacket);
			System.out.print("\nMessage received from the client.");
			InetAddress clientAddress = receivePacket.getAddress();    // client's IP address
			int clientPort = receivePacket.getPort();                  // client's port number
			int dataLength = receivePacket.getLength();                // the number of received bytes
			
			String request = new String(receivedMessage, 0, dataLength);
			System.out.println("The received request message is: \n"+request);
			String reqMsg=SepIntCheck(request);	
			int checksum=integrityCheck(reqMsg);	 
			int intCheck=ExtractChecksum(request);
			int responseCode;
			boolean rescode = SyntaxCheckFile(request);	//to check whether the received request is syntactically correct
			if(checksum!=intCheck)	//if the calculated and received integrity check values do not match
				responseCode=1;
			else if(rescode==false)
				responseCode=2;	
			else if(!(request.substring(5,8).equals("1.0")))	//to check whether the protocol version in the received request is correct
				responseCode=4;
			else
				responseCode=0;	//the received request is valid
			String responseMsg=AssembleResMsg(request, responseCode);
			byte[] resBytes=responseMsg.getBytes();
			DatagramPacket resPacket = new DatagramPacket(resBytes, resBytes.length, clientAddress, clientPort); 
			serverSocket.send(resPacket); 
			System.out.println("Response message sent to the client."); 
		}
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
	
	public static int ExtractChecksum(String request){
		int k=0;
		int o=0;
		int strlen=SYMBOL.length();
		int n=request.indexOf(SYMBOL);
		k=request.indexOf( SYMBOL, n+1 );
		o=request.indexOf( SYMBOL, k+1 );
		String resText1 = request.substring(k+strlen,o);  
		int checksum = Integer.parseInt(resText1);
		return checksum;
	}
	
	public static String SepIntCheck(String request)
	{
		int k=0;
		int strlen=SYMBOL.length();
		int n=request.indexOf(SYMBOL);
		k=request.indexOf( SYMBOL, n+1 );
		String request1 = request.substring(0,k+strlen);  
		return request1;
	}
	
	public static String AssembleResMsg(String request, int responseCode){
		String responseMsg;
		String content;
		switch(responseCode)
		{
			case 0:
				String path="C:\\Users\\Dell\\Desktop\\";
				int k=0;
				int strlen=SYMBOL.length();
				int n=request.indexOf(SYMBOL);
				k=request.indexOf( SYMBOL, n+1 );
				String fileName = request.substring(n+strlen,k);  
				path=path+fileName;	
				try{
					content="";
					BufferedReader reader = new BufferedReader(new FileReader(path));
					String line = null;
					while ((line = reader.readLine()) != null) {
						content=content+"\n"+line;
					}
					reader.close();
					System.out.println("OK. The response has been created according to the request.");
				}
				catch(Exception e)
				{
					System.out.println("Error: non-existent file. The file with the requested name does not exist.");
					responseCode=3;	//file is considered non existent when reading operation fails
					content="";	//empty content
					break;
				}
				break;
			case 1:
				System.out.println("Error: integrity check failure. The request has one or more bit errors.");
				content="";
				break;
			case 2:
				System.out.println("Error: malformed request. The syntax of the request message is not correct.");
				content="";
				break;
			case 4:
				System.out.println("Error: wrong protocol version. The version number in the request is different from 1.0.");
				content="";
				break;
			default:
				content="";
				break;
		}		
		
		int contentLength=content.length();
		String conLength = Integer.toString(contentLength);
		responseMsg=PROTOCOL+" Response"+SYMBOL+responseCode+SYMBOL+conLength+SYMBOL+content+SYMBOL;
		int intCheck= integrityCheck(responseMsg);
		String checksum = Integer.toString(intCheck);
		responseMsg=responseMsg+checksum+SYMBOL;
		return responseMsg;
	}
	public static boolean SyntaxCheckFile(String s)
	{
		int k=0;
		int o=0, n=0;
		int strlen=SYMBOL.length();
		int strlen1=PROTOCOL.length();
		
		o=s.indexOf(SYMBOL);	//first occurrence of [CR+LF] 
		n=s.indexOf(SYMBOL, o+1); 	//second occurrence of [CR+LF] 
		k=s.indexOf(SYMBOL, n+1);	//third occurrence of [CR+LF] 
		String req = s.substring(PROTOCOL.length()+1,o);
		String FullFile = s.substring(o+SYMBOL.length(),n);
		String [] str = FullFile.split("\\.");
		String File= str[0];
		String Extension= str[1];
		String integrityval = s.substring(n+SYMBOL.length(),k);
	
		boolean flag=true;
		if(o<0 || k<0 || n<0) //if [CR+LF] is not present thrice
		{
			System.out.println("Syntax error while assembling the request");
			flag=false;
		}
		else if(req.equals("Request")==false)	//if the word "request" is absent
		{
			System.out.println("Syntax error in the word 'Request'");
			flag=false;
		}	
		else if(File.charAt(0)<65 || 122<File.charAt(0) || (90<File.charAt(0) && File.charAt(0)<97)) 
		{
			System.out.println("Syntax error in first letter of the file name");
			flag=false;
		}
		else
		{
			for(int i=1; i<File.length(); i++)
			{
				if(File.charAt(i)<48 || (57<File.charAt(i) && File.charAt(i)<65)|| (90<File.charAt(i) && File.charAt(i)<95) || (95<File.charAt(i) && File.charAt(i)<97) || 122<File.charAt(i)) 
				{
					System.out.println("Syntax error in the file name");
					flag=false;
				}
			}
			
			for(int i=0; i<Extension.length(); i++)
			{
				if(Extension.charAt(i)<48 || (57<Extension.charAt(i) && Extension.charAt(i)<65)|| (90<Extension.charAt(i) && Extension.charAt(i)<97) || 122<Extension.charAt(i))
				{
					System.out.println("Syntax error in the file extension");
					flag=false;
	
				}
			}
			
			for(int i=0; i<integrityval.length(); i++)
			{
				if(integrityval.charAt(i)<48 || 57<integrityval.charAt(i)) 
				{
					System.out.println("Syntax error in the integrity value");
					flag=false;
				}
			}
		}
		return flag;
	}
}
