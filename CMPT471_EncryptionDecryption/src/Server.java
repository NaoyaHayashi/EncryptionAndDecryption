import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


/**
	* Server class waits TCP/IP connection from the Client. <br>
	* The server will report if the connection from the client is successfully built. <br>
	* Then, it will receive the number given by client and replies with Yb. <br>
	* The server computes Kb using Diffie-Hellman algorithm. <br>
	* After that, the server receives encrypted message from client.
	* The server decrypts the message and display both the encrypted and decrypted(original) message on the console.
	* The server will close the connection after sending the reply.

 	* @author Naoya Hayashi
 	* <dt><b>Student No.:</b><dd>
 	* 301233985
 	* @date November 23rd, 2015
 	* @version 1.0
 	*/
public class Server {
	
	// Array of characters used for substitution encryption
	static char[] A = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
			'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ',', '.', '?', ' '};
	
	/**
	 * Main method for the server.
	 * @param args args[0] for port
	 * <dt><b>Precondition:</b><dd>
	 * the given port number must not be the reserved numbers and exceed 65536.
	 */
	public static void main(String[] args) throws IOException {
		

		// These constants are public numbers shared by both client and server to derive a secret key using Diffie-Hellman algorithm.
		final int q = 15; // The number 15 and 3 are my arbitrary choice.
		final int n = 3;
		final int N = 5;
		int Xb = (int) ((Math.random() + 0.1) * 10); // Xb is a random number between 1 and 10
		System.out.println("Xb is " + Xb);
		// Compute Yb
		int Yb = ((int) Math.pow(n, Xb)) % q;
		System.out.println("Yb is " + Yb);

		// Check if the number of input is 1 or not.
		// If it is not 1, the server will not launch.
		if(args.length != 1){
			System.out.println("Invalid argument!! Provide just a port number.");
			System.out.println("The program ends...");
			System.exit(1);
		}
		int port = 8888; // temporary value assigned to suppress the syntax warning
		// If the port is not number, it will end.
		try{
			port = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException e){
			System.out.println("The argument can't be converted into an integer!!");
			System.out.println("The program ends...");
			System.exit(1);
		}
				
		// Create a server socket
		ServerSocket serverSocket = new ServerSocket(port);
		try{
			while(true){
				System.out.println("Waiting for clients to connect . . . ");
				// The server accepts connections if anything.
				Socket socket = serverSocket.accept();
				System.out.println("Client connected.");
				// Get streams
				InputStream instream = socket.getInputStream();
				OutputStream outstream = socket.getOutputStream();
				// Turn streams into a scanner and a writer
				Scanner input = new Scanner(instream);
				PrintWriter output = new PrintWriter(outstream);
				String str = "";
				
				boolean firstLoop = true;
				// Terminator String
				// If the server recognizes this terminator, it knows no more messages from client.
				String EOF_token = "\\EOF";
				while(input.hasNext()){
					// Get the client message word by word.
					String nextStr = input.next();
					if(nextStr.equals(EOF_token)){
						break;
					}
					// If the loop is executed first time, it won't concatenate with a whitespace, " ". 
					if(firstLoop){
						str = nextStr;
						firstLoop = false;						
					}
					else{
						str = str + " " + nextStr;
					}
				}
				// Display the message sent by the client.
				System.out.print("The value of Ya sent from the Client: ");
				System.out.println(str);
				
				// sending reply message to the client
				String data = Yb + " " + EOF_token + "\n";
				output.write(data);
				output.flush();
				
				// Compute Kb (secret key)
				int Ya = Integer.parseInt(str);
				int Kb = ((int) Math.pow(Ya, Xb)) % q;
				System.out.println("The value of Kb (secret key): " + Kb);
				
				
				str = ""; // reset str
				firstLoop = true; // reset firstLoop
				while(input.hasNext()){
					// Get the client message word by word.
					String nextStr = input.next();
					if(nextStr.equals(EOF_token)){
						break;
					}
					// If the loop is executed first time, it won't concatenate with a whitespace, " ". 
					if(firstLoop){
						str = nextStr;
						firstLoop = false;						
					}
					else{
						str = str + " " + nextStr;
					}
				}
				System.out.print("The encrypted message received from Client: ");
				System.out.println(str);
				
				// First step of decryption (Reverse-transpose)
				String transposedMessage = reverseTranspose(str);
				System.out.print("The reverse-transposed message: ");
				System.out.println(transposedMessage);
				
				// Second step of decryption (Reverse-substitution)
				int i = (Kb % N) + 1; // Compute i, which determines scheme for substitution (Si where 1<=i<=5)
				String decryptedMessage = "";
				char ch;
				for(int k=0; k<transposedMessage.length(); k++){
					ch = reverseSubstitue(transposedMessage.charAt(k), i);
					decryptedMessage = decryptedMessage + ch;
				}
				// Display the decrypted (original) message.
				System.out.print("The decrypted (reverse-substituted) message: ");
				System.out.println(decryptedMessage);
				
				// close the input stream and socket connected to the client
				input.close();
				socket.close();
			}
		}
		finally{
			// close the server socket
			// Program normally won't reach here but this is necessary to suppress syntax warning.
			serverSocket.close();
			System.exit(0);
		}
	}
	
	
	/**
	* Reverse-transpose every sequence of 16 characters (4*4 array) by the scheme specified in my document, and return the message. <br>
	* @param message a message to be reverse-transposed
	* @return a reverse-transposed message in String
	* <dt><b>Precondition:</b><dd>
	* message must not be null.
	*/
	public static String reverseTranspose(String message){
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
	
	
	/**
	* Reverse-substitute a encrypted character with the original based on the substitution scheme defined in the document. <br>
	* @param ch an encrypted character
	* @param i a number to specify which substitution scheme is used
	* @return a decrypted character
	* <dt><b>Precondition:</b><dd>
	* ch must be a valid character (i.e. must be one of the characters specified in my design)
	* i must be an integer from 1 to 5
	*/
	public static char reverseSubstitue(char ch, int i){
		for(int j=0; j<A.length; j++){
			if(A[j] == ch){
				int k = j - i;
				if(k < 0){
					k = 30 + k;
				}
				return A[k];
			}
		}
		return '#'; // return an invalid character if ch doesn't match any of characters in array A
	}

}
