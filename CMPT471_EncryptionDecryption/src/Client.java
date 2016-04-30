import java.io.*;
import java.net.Socket;
import java.util.Scanner;


/**
 	* Client class requests TCP/IP connection to the Server. <br>
 	* If the connection is built successfully, this client first sends a number Ya to Server. <br>
 	* The client expects that the server replies with Yb for Diffie-Hellman algorithm.<br>
 	* If the reply is received by the client, the number will be displayed on the console. <br>
 	* Secondly, client asks the user to type a message, and encrypts it using substitution and transposition. <br>
 	* Then, client sends the encrypted message to Server. <br>
 	* The client will terminate after sending the encrypted message. <br>
 	* Note: Before you run this program, Serve.java must be running; otherwise, the client can't connect to the server.

	* @author Naoya Hayashi
	* <dt><b>Student No.:</b><dd>
	* 301233985
	* @date November 23rd, 2015
	* @version 1.0
	*/
public class Client {
	// Array of characters used for substitution encryption
	static char[] A = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ',', '.', '?', ' '};
	
	/**
	 * Main method for a client to build TCP/IP connection with a server. <br>
	 * @param args args[0] for IP address of a server to connect, and args[1] for port
	 * <dt><b>Precondition:</b><dd>
	 * Command-line input for IP address and port number must be valid.
	 * Especially, the given IP address must exist and reachable. 
	 * Also, the given port number must not be the reserved numbers and exceeds 65536.
	 */
	public static void main(String[] args) throws IOException {
		String serverIP;
		int port = 8888; // temporarily assigned
		
		// Check if the number of arguments is valid or not
		if(args.length != 2){				
			System.out.println("The given arguments are invalid!");
			System.out.println("Provide just two arguments: valid Server IP Address and Port");
			System.out.println("The program ends...");
			System.exit(1);
		}
		
		
		// These constants are public numbers shared by both client and server to derive a secret key using Diffie-Hellman algorithm.
		final int q = 15; // The number 15 and 3 are my arbitrary choice.
		final int n = 3;
		final int N = 5;
		int Xa = (int) ((Math.random() + 0.1) * 10); // Xa is a random number between 1 and 10
		System.out.println("Xa is " + Xa);
		
		// Compute Ya
		int Ya = ((int) Math.pow(n, Xa)) % q;
		System.out.println("Ya is " + Ya);
		
		// Get command-line arguments
		serverIP = args[0];
		// Check if the given input for port is a valid number; otherwise, terminates the program.
		try{
			port = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e){
			System.out.println("The argument can't be converted into an integer!!");
			System.out.println("The program ends...");
			System.exit(1);
		}
		// Open a socket with given arguments
		Socket socket = new Socket(serverIP, port);
				
		// Get streams
		InputStream instream = socket.getInputStream();
		OutputStream outstream = socket.getOutputStream();
				
		// Turn streams into a scanner and a writer
		Scanner input = new Scanner(instream);
		PrintWriter output = new PrintWriter(outstream);
		
		
		// Send a message to the server
		// "\\EOF" is a terminator to tell the server that it's the end of the message.
		// The sever will know no more incoming messages, so it can proceed.
		String EOF_token = "\\EOF";
		String data = Ya + " " + EOF_token + "\n"; // Sending Ya to the Server
		output.print(data);
		output.flush();
				
		
		String response = "";
		boolean firstLoop = true;
		// Read the server's response if anything
		while(input.hasNext()){
			String incomingData = input.next();
			
			if(incomingData.equals(EOF_token)){
				// Display Yb sent from the Server
				System.out.print("The value of Yb sent from Server: ");
				System.out.println(response);
				break;
			}
			
			if(firstLoop){
				response = incomingData;
				firstLoop = false;						
			}
			else{
				response = response + " " + incomingData;
			}
		}
		
		// Compute Ka (secret key)
		int Yb = Integer.parseInt(response);
		int Ka = ((int) Math.pow(Yb, Xa)) % q;
		System.out.println("The value of Ka (secret key): " + Ka);
		
		response = ""; // reset response
		firstLoop = true; // reset firstLoop
		
		// Get a message input from the user
		Scanner in = new Scanner(System.in);
		System.out.print("Type a message you wish to send and then press enter: ");
		String message = in.nextLine();
		in.close();

		
		// Message modification
		// The message must be divisible by 16 (to fit 4*4 array) for the later operations (encryption).
		// If the length of the message typed by the user is not divisible by 16, append whitespace to make its length divisible by 16.
		while(message.length() % 16 != 0){
			message = message + ' '; // append whitespace
		}
		
		// Substitution Encryption
		int i = (Ka % N) + 1; // Compute i, which determines scheme for substitution (Si where 1<=i<=5)
		System.out.println("The value of i for substitution scheme: " + i);
		char ch;
		String substitutedMessage = "";
		for(int k=0; k<message.length(); k++){
			ch = substitute(message.charAt(k), i);
			substitutedMessage = substitutedMessage + ch;
		}
		System.out.println("The substituted message: " + substitutedMessage);
		
		// Transposition Encryption
		String encryptedMessage = transpose(substitutedMessage);
		System.out.println("The transposed (encrypted) message: " + encryptedMessage);
		
		// Send encrypted message to the Server
		data = encryptedMessage + " " + EOF_token + "\n";
		output.print(data);
		output.flush();
		
		// Close the inputstream and socket/connection
		input.close(); 
		socket.close();
		System.exit(0);
	}

	/**
	* Substitute a character with another based on the substitution scheme defined in the document. <br>
	* @param ch a character to be substituted
	* @param i a number to specify which substitution scheme is used
	* @return a substituted character
	* <dt><b>Precondition:</b><dd>
	* ch must be a valid character (i.e. must be one of the characters specified in my design)
	* i must be an integer from 1 to 5
	*/
	public static char substitute(char ch, int i){
		for(int j=0; j<A.length; j++){
			if(A[j] == ch){
				int k = j + i; // Shift the character by i to the next
				if(k >= 30){
					k = k % 30; // If k is 30 or greater, it will cause OutOfBounds exception, so use mod to handle the case.
				}
				return A[k];
			}
		}
		return '#'; // return an invalid character if ch doesn't match any of characters in array A (This character ('#') is not supposed to appear on anywhere on output)
	}
	
	
	/**
	* Transpose every sequence of 16 characters (4*4 array) by the scheme specified in my document, and return the transposed message. <br>
	* @param message a message to be transposed
	* @return a transposed message in String
	* <dt><b>Precondition:</b><dd>
	* message must not be null.
	*/
	public static String transpose(String message){
		String str = "";
		int r = 4;
		int n = 4;
		int chunk = r * n; // One chunk of data is 16 because the array is 4*4 based on my design.
		int iteration = message.length() / chunk;
		
		// Loop for every 16 characters
		for(int t=0; t<iteration; t++){
			// Transpose the characters 
			str = str + message.charAt((0+(chunk*t)));
			str = str + message.charAt((4+(chunk*t)));
			str = str + message.charAt((8+(chunk*t)));
			str = str + message.charAt((12+(chunk*t)));
			str = str + message.charAt((1+(chunk*t)));
			str = str + message.charAt((5+(chunk*t)));
			str = str + message.charAt((9+(chunk*t)));
			str = str + message.charAt((13+(chunk*t)));
			str = str + message.charAt((2+(chunk*t)));
			str = str + message.charAt((6+(chunk*t)));
			str = str + message.charAt((10+(chunk*t)));
			str = str + message.charAt((14+(chunk*t)));
			str = str + message.charAt((3+(chunk*t)));
			str = str + message.charAt((7+(chunk*t)));
			str = str + message.charAt((11+(chunk*t)));
			str = str + message.charAt((15+(chunk*t)));
		}
		
		return str;
	}
	
}
