  /**
   * @author Xiaofei Zhang
   */



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

//  Apr 25 version

//class InvalidRequestException extends Exception {
//	public InvalidRequestException() {
//		super("ERROR: invalid request");
//	}
//}
//
//class ShutdownException extends Exception {
//	
//}

public class Server extends ServerSocket implements Runnable {

	public int port;
	public Incoming request;

	ArrayList<Incoming> requestsPending = new ArrayList<Incoming>();
	

	private List<String> fromList = Arrays.asList("CL50", "EE", "LWSN",
			"PMU", "PUSH");
	private List<String> toList = Arrays.asList("CL50", "EE", "LWSN",
			"PMU", "PUSH", "*");
    private List<String> pendingToList = Arrays.asList("CL50", "EE", "LWSN",
            "PMU", "PUSH");
    
	private final String[] fromlisting = {"CL50", "EE", "LWSN", "PMU", "PUSH", "PUSH"};
	private final String[] tolisting = {"CL50", "EE", "LWSN", "PMU", "PUSH", "*"};
		
	/**
	 * Construct the server, and create a server socket,
	 * bound to the specified port...
	 * 
	 * @throws IOException IO error when opening the socket.
	 */
	public SafeWalkServer(int port) throws IOException {
	    //TODO: finish the method
		super(port);
		setReuseAddress(true);
	}
	 
	/**
	 * Construct the server, and create a server socket, 
	 * bound to a port that is automatically allocated.
	 * 
	 * @throws IOException IO error when opening the socket.
	 */
	public Server() throws IOException {
	    //TODO: finish the metho
		this(0);
		System.out.println("Port not specified. Using free port " + getLocalPort() + ".");
	
	}
	
		
	public void run() {
		try {
			while(!isClosed()) {
				Socket client = accept();
				request = new Incoming(client);
				request.run();
			}
		
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}
		
	/**
	 * Return true if the port entered by the user is valid. Else return false. 
	 * Return false if you get a NumberFormatException while parsing the parameter port
	 * Call this method from main() before creating SafeWalkServer object 
	 * Note that you do not have to check for validity of automatically assigned port
	 */
	public static boolean isPortValid(String port) {
		//TODO: finish this method
		boolean isValid;
		int portNum;
		if (port.equals("")) 
			return false;
		
		try {
			portNum = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			//System.out.println("You typed in part number in wrong format");
			return false;
		}
		
		if (portNum >= 1025 && portNum <= 65535 ) {
			isValid = true;
		} else {
			isValid = false;
		}
		return isValid;
	}
	
	

	
	public static void main(String[] args) {
		boolean isvalid = false;
		SafeWalkServer sws = null;
		
		if (args.length == 0) {
			isvalid = false;
		} else {
			
            isvalid = isPortValid(args[0]);
        }
		

		try {
			if (isvalid) {
				sws = new Server(Integer.parseInt(args[0]));			
			} else {
				sws = new Server();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Thread serverThread = new Thread(sws);
		serverThread.start();
	}
	
	
	class Incoming implements Runnable {

		Socket socket;
		BufferedReader in;
		PrintWriter out;
		String clientInput;
		String[] sepClientInput;
		
		private String name = null;
		private String from = null; 
		private String to = null;
		
		public String getName() {
			return name;
		}


		public void setName(String name) {
			this.name = name;
		}


		public String getFrom() {
			return from;
		}


		public void setFrom(String from) {
			this.from = from;
		}


		public String getTo() {
			return to;
		}


		public void setTo(String to) {
			this.to = to;
		}


			
			public Incoming(Socket socket) {
				this.socket = socket;
		
				try {	
					
					in = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));
					out = new PrintWriter(socket.getOutputStream(), true);
					
					clientInput = in.readLine();

				} catch (IOException e) {

						e.printStackTrace();
				}
				
			}
	
			
			public void run(){

				boolean inputValid = false;
		
				if (isInputValid(clientInput)){
					sepClientInput = clientInput.split(",");
					inputValid = true;
				} 
		
				// Parse Input for both command and request
				try {
					if (inputValid) {
						if (sepClientInput[0].equals(":RESET")) {
								this.reset(socket, out);
						} else if (sepClientInput[0].equals(":SHUTDOWN")) {
								this.shutdown(socket, out);
						} else if (sepClientInput[0].equals(":PENDING_REQUESTS")){
								int pendingTaskNum = this.isPendingValid(sepClientInput);
								this.conductPendingTask(socket, out, pendingTaskNum, sepClientInput);
										
						} else {
	
							name = sepClientInput[0];
							from = sepClientInput[1];
							to = sepClientInput[2];
					
							Incoming match = this.pair(request);
							if (match != null) {
								String response = "RESPONSE: %s";
								out.println(String.format(response, match.toString()));
								match.out.println(
										String.format(response, request.toString()));
								requestsPending.remove(match);
								match.socket.close();
								socket.close();
							} else {
								requestsPending.add(request);
							}
						}
					}
				} catch (IOException e){
					e.printStackTrace();
				}
		
			}
			
			/////////////////////////////////
			//////// double check output value
			//////// decide whether socket needs to be closed or not
			/////////////////////////////////
			
			// conductPendingTask
			private void conductPendingTask (Socket clientSocket, PrintWriter out, int requestTaskNum, String[] sepClientInput) {
				//System.out.println(requestTaskNum);
				switch (requestTaskNum) {
					case -1:
						out.println("ERROR: invalid command");
						break;
					case 1:
						this.conductPendingTaskOne(clientSocket, out);
						break;
					case 2:
						this.conductPendingTaskTwo(clientSocket, out, sepClientInput);
						break;
					case 3:
						this.conductPendingTaskThree(clientSocket, out, sepClientInput);
						break;
					case 4:
						this.conductPendingTaskFour(clientSocket, out);
						break;
					/////////////////////////
					//////// might need default error
					/////////////////////////
					
				}
			}
			
			//////////// Those are the separate Pending Tasks 
			private void conductPendingTaskOne (Socket clientSocket, PrintWriter out) {
				//out.println("RESPONSE: " + " of pending requests ");
				int totalPendingNum = 0;
				
				Iterator<Incoming> it = requestsPending.iterator();
				while (it.hasNext()) {
					it.next();
					totalPendingNum++;
				}
				out.println("RESPONSE: # of pending requests = " + totalPendingNum);
				//out.println(" = " + totalPendingNum);
				//out.println();
			}

			private void conductPendingTaskTwo (Socket clientSocket, PrintWriter out,
					String[] sepClientInput) {
				//out.print("RESPONSE: # of pending requests from ");
				
				int totalFrom = 0;
				String from = sepClientInput[2];
				//System.out.println("from[][][][][][][] " + from);
				Iterator<Incoming> it = requestsPending.iterator();
				while (it.hasNext()) {
					Incoming client = it.next();
					if (client.getFrom().equals(from)) {
						totalFrom++;
					}
				}
				out.println("RESPONSE: # of pending requests from " + from + " = " + totalFrom);
				//out.println();
			}
			
			private void conductPendingTaskThree (Socket clientSocket, PrintWriter out,
					String[] sepClientInput) {
				//out.println("RESPONSE: # of pending requests to ");
				int totalTo = 0;
				String to = sepClientInput[3];
				
				Iterator<Incoming> it = requestsPending.iterator();
				while (it.hasNext()) {
					Incoming client = it.next();
					if (client.getTo().equals(to)) {
						totalTo++;
					}
				}
				//out.println(to + " = " + totalTo);
				out.println("RESPONSE: # of pending requests to " + to + " = " + totalTo);
				//out.println();
			}
					
			private void conductPendingTaskFour (Socket clientSocket, PrintWriter out) {
				out.print("[");
				Iterator<Incoming> it = requestsPending.iterator();
				while (it.hasNext()) {
					Incoming client = it.next();
					out.print(client.toString("[%s, %s, %s]"));
					if (it.hasNext())
						out.print(", ");
				}
				out.println("]");
			}
			
			/////////// This is the pair method
			private Incoming pair (Incoming request) {
				Iterator<Incoming> that = requestsPending.iterator();
				
				while (that.hasNext()) {
					
					Incoming ca = that.next();
					
					if (request.getFrom().equals(ca.getFrom())) {
						if ((ca.getTo().equals("*") && !request.getTo().equals("*"))
								|| (request.getTo().equals("*") && !ca.getTo().equals("*"))
								|| (request.getTo().equals(ca.getTo()) && !request
										.getTo().equals("*"))) {
							return ca;
						}
						
					}
					
				}
				return null;
			}
			
			private void shutdown(Socket clientSocket, PrintWriter out) throws IOException{
				this.reset(clientSocket, out);
				close();
			}

			private void reset(Socket clientSocket, PrintWriter out) throws IOException {
				Iterator<Incoming> it = requestsPending.iterator();
				while (it.hasNext()) {
					Incoming client = it.next();
					client.out.println("ERROR: connection reset");
					client.socket.close();
				}
				out.println("RESPONSE: success");
			}
			
		    // To judege with string in the listArray, which is used for checking elements in test 
		    public boolean isInList (List<String> testString, String message) {
		        boolean result = false;
		        Iterator<String> testString1 = testString.iterator();
		        while (testString1.hasNext()){
		            if (message.equals(testString1.next())) {
		                result = true;
		                break;
		            }
		        }
		        return result;
		    }

		    // To judge weather pendingRequest is valid
		    public boolean isReqValid (String[] req) {
		        boolean result = false;

		        if(req == null ) {
		            return false;
		        }

		         if (req.length == 3 && !req[1].equals(req[2]) 
		                && isInList(fromList, req[1]) && isInList(toList, req[2])) {
		                    result = true;
		                }
		        return result;   

		    }

		    // To test if pending valid and specify pending task
		    public int isPendingValid (String[] pending) {
		        int taskNum = -1;

		        if(pending == null || pending.length != 4) {
		            return taskNum;
		        }

		        if (pending[0].equals(":PENDING_REQUESTS")) { 
		            if(pending[1].equals("#")) {
		                if (pending[2].equals("*") && pending[3].equals("*")) {
		                    taskNum = 1;
		                } else if (isInList(fromList, pending[2]) && pending[3].equals("*")) {
		                    taskNum = 2;
		                } else if (pending[2].equals("*") && isInList(pendingToList, pending[3])) {
		                    taskNum = 3;
		                }
		            } else if (pending[1].equals("*") && pending[2].equals("*") && pending[3].equals("*")) {
		                    taskNum = 4;
		            }
		        }
		       
		        return taskNum;
		    }
         
		    // check request invalidity 
		   	public boolean isValidMessage(String message) {
		   		String[] msgarray = message.split(",");

		   		boolean from = false;
		   		boolean to = false;
		   		if (msgarray.length == 3) {
		   			for (int i = 0; i < fromlisting.length; i++) {
		   				if (msgarray[1].equals(fromlisting[i])) {
		   					from = true;
		   				}
		   				if (msgarray[2].equals(tolisting[i])) {
		   					to = true;
		   				}
		   			}
		   			if (from == true && to == true && !msgarray[1].equals(msgarray[2])) {
		   				return true;
		   			}
		   		}
		   		return false;	
		   	}
    
		  	public boolean isInputValid(String msg) {
		  			
		  			if (msg == null || msg.length() == 0) {
		  				return false;
		  			}
		  			
		  			if (msg.split(",")[0].equals(":PENDING_REQUESTS") && msg.split(",").length != 4) {
		  				out.println("ERROR: invalid command");
		  				return false;
		  			}
		  			
		  			if (msg.startsWith(":")) {
		  				if (msg.equals(":RESET") || msg.equals(":SHUTDOWN") ||
		  						msg.split(",")[0].equals(":PENDING_REQUESTS")) {
		  						return true;
		  				}
		  				
		  				else {
		  					out.println("ERROR: invalid command");
		  					return false;
		  				}
		  			}
		  			else {
		  				if (!isValidMessage(msg)) {
		  					out.println("ERROR: invalid request");
		  					return false;
		  				}
		  				
		  				return true;
		  			}
		  			
		  	  	}

		    
			public String toString(String format) {
				return String.format(format, this.getName(), this.getFrom(), this.getTo());
			}
			
			public String toString() {
				return this.toString("%s,%s,%s");
			}
			
	}
		
	
	class InvalidRequestException extends Exception {
		public InvalidRequestException() {
			super("ERROR: invalid request");
		}
	}
	
	
	class IllegalException extends Exception {
	    public IllegalException (String message) {
	        super(message);
	    }
	}

	
	
}
	
	





















