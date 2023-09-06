import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.opencsv.CSVReader;

public class CSVSearchClientOverlap
{
   public static void main(String[] args)
   {
      ClientsHandler clientsHandler = new ClientsHandler();
   }
}

//builds the array list of all clients by using CSVReader
//provides functions for manipulating the clients arraylist
class ClientsHandler
{
   private String filePathProgramEntryExits = "ProgramEntryExits.csv";
   //private String filePathServices = "services.csv";
   private ArrayList<Client> clients;
   
   //columns
   private int entryExitClientIDCol;
   private int entryDateCol;
   private int exitDateCol;
   
   //constructor
   public ClientsHandler()
   {
      clients = new ArrayList<Client>();
      List<String[]> listProgramEntryExits = null; //CSVReader prefers to read data into a list format from a CSV
      
      try
      {
         //read both CSV files into a list
         CSVReader csvReader = new CSVReader(new FileReader(new File(filePathProgramEntryExits)));
         listProgramEntryExits = csvReader.readAll();
         csvReader.close();
      } catch (Exception e)
      {
         e.printStackTrace();
      }
      
      //read through each column to get column headers and pick columns
      for(int i = 0; i < listProgramEntryExits.get(0).length; i++)
      {
         String columnHeader = listProgramEntryExits.get(0)[i];
         
         if(columnHeader.equals("Client ID"))
         {
            entryExitClientIDCol = i;
         }
         else if(columnHeader.equals("Entry Date"))
         {
            entryDateCol = i;
         }
         else if(columnHeader.equals("Exit Date"))
         {
            exitDateCol = i;
         }
      }
      
      //custom comparators that sort based on clientID column
      Comparator<String[]> cmpProgramEntryExits = new Comparator<String[]>()
      {
         @Override
         public int compare(String[] o1, String[] o2)
         {
            return Integer.valueOf(o1[entryExitClientIDCol]).compareTo(Integer.valueOf(o2[entryExitClientIDCol]));
         }
      };
      
      //sort list by clientID, but skip first row (columnHeader row) by using a subList
      //important to sort array so that multiple rows can be attributed to one client
      Collections.sort(listProgramEntryExits.subList(1, listProgramEntryExits.size()), cmpProgramEntryExits);
      
      //start populating list of clients from ProgramEntryExits
      int previousClientID = -1;
      int clientIndex = -1;
      //loop through every row of the list. each row represents a program entry and exit date for a specific client
      for (int i = 1; i < listProgramEntryExits.size(); i++)
      {
         //all data read in from CSVReader is in the string format
         int clientID = Integer.parseInt(listProgramEntryExits.get(i)[entryExitClientIDCol]);
         
         //only create a new client object when next row has a different clientID from the previous row (possible due to sorting)
         if (previousClientID != clientID)
         {
            clients.add(new Client(clientID));
            clientIndex = clients.size() - 1; //newest client will always be at end of clients arraylist
         }
         clients.get(clientIndex).addProgramEntryExit(
               i,
               listProgramEntryExits.get(i)[entryDateCol],
               listProgramEntryExits.get(i)[exitDateCol]);
         
         previousClientID = clientID;
      }
      
      sortClients();
      
      findOverlapDates();
      createOutputFileOverlap();
   }
   
   //searches through every programEntryExit for a single client looking for overlapping date ranges
   //an overlapping date range indicates that a client was/is in two programs concurrently
   public void findOverlapDates()
   {
      /*
      4 programEntryExits example
      0123
      will search for matches between:
      01
      02
      03
      12
      13
      23
      */
      for (int i = 0; i < clients.size(); i++)
      {
         for (int j = 0; j < clients.get(i).getProgramEntryExits().size(); j++)
         {
            for (int k = j + 1; k < clients.get(i).getProgramEntryExits().size(); k++)
            {
               ProgramEntryExit programEntryExit = clients.get(i).getProgramEntryExits().get(j);
               Date entryDate = programEntryExit.getEntryDate();
               Date exitDate = programEntryExit.getExitDate();
               
               ProgramEntryExit programEntryExit2 = clients.get(i).getProgramEntryExits().get(k);
               Date entryDate2 = programEntryExit2.getEntryDate();
               Date exitDate2 = programEntryExit2.getExitDate();
               
               //explanation of logic at https://chandoo.org/wp/date-overlap-formulas/
               //2 currently open programs
               if (exitDate == null && exitDate2 == null)
               {
                  //overlap established
                  //check where overlap begins, entryDate or entryDate2
                  if (entryDate.after(entryDate2) || entryDate.equals(entryDate2))
                  {
                     //use entryDate for overlapEntryDate
                     //check for existing overlap
                     if (programEntryExit.getOverlapID() != -1)
                     {
                        //create copy if overlap already exits for this row
                        clients.get(i).addCopyProgramEntryExit(
                              programEntryExit.getRowID(),
                              programEntryExit.getEntryDate(),
                              programEntryExit.getExitDate());
                        
                        //newest copied programEntryExit always located at end of arraylist
                        int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                        ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                        copyProgramEntryExit.setCopy(true);
                        
                        copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                        copyProgramEntryExit.setOverlapType("2 open programs");
                        copyProgramEntryExit.setOverlapEntryDate(entryDate);
                     }
                     else
                     {
                        programEntryExit.setOverlapID(programEntryExit2.getRowID());
                        programEntryExit.setOverlapType("2 open programs");
                        programEntryExit.setOverlapEntryDate(entryDate);
                     }
                  }
                  else if (entryDate.before(entryDate2))
                  {
                     //use entryDate2 for overlapEntryDate
                     //check for existing overlap
                     if (programEntryExit.getOverlapID() != -1)
                     {
                        //create copy
                        clients.get(i).addCopyProgramEntryExit(
                              programEntryExit.getRowID(),
                              programEntryExit.getEntryDate(),
                              programEntryExit.getExitDate());
                        
                        int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                        ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                        copyProgramEntryExit.setCopy(true);
                        
                        copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                        copyProgramEntryExit.setOverlapType("2 open programs");
                        copyProgramEntryExit.setOverlapEntryDate(entryDate2);
                     }
                     else
                     {
                        programEntryExit.setOverlapID(programEntryExit2.getRowID());
                        programEntryExit.setOverlapType("2 open programs");
                        programEntryExit.setOverlapEntryDate(entryDate2);
                     }
                  }
               }
               
               if (exitDate != null && exitDate2 == null)
               {
                  //program1 exit date is within currently running program2 - overlap
                  if (exitDate.after(entryDate2) || exitDate.equals(entryDate2))
                  {
                     //overlap established
                     //check where overlap begins, entryDate or entryDate2
                     if (entryDate.after(entryDate2) || entryDate.equals(entryDate2))
                     {
                        //check for existing overlap
                        if (programEntryExit.getOverlapID() != -1)
                        {
                           //create copy
                           clients.get(i).addCopyProgramEntryExit(
                                 programEntryExit.getRowID(),
                                 programEntryExit.getEntryDate(),
                                 programEntryExit.getExitDate());
                           
                           int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                           ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                           copyProgramEntryExit.setCopy(true);
                           
                           copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                           copyProgramEntryExit.setOverlapType("within open program");
                           copyProgramEntryExit.setOverlapEntryDate(entryDate);
                           copyProgramEntryExit.setOverlapExitDate(exitDate);
                        }
                        else
                        {
                           programEntryExit.setOverlapID(programEntryExit2.getRowID());
                           programEntryExit.setOverlapType("within open program");
                           programEntryExit.setOverlapEntryDate(entryDate);
                           programEntryExit.setOverlapExitDate(exitDate);
                        }
                     }
                     else if (entryDate.before(entryDate2))
                     {
                        //check for existing overlap
                        if (programEntryExit.getOverlapID() != -1)
                        {
                           //create copy
                           clients.get(i).addCopyProgramEntryExit(
                                 programEntryExit.getRowID(),
                                 programEntryExit.getEntryDate(),
                                 programEntryExit.getExitDate());
                           
                           int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                           ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                           copyProgramEntryExit.setCopy(true);
                           
                           copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                           copyProgramEntryExit.setOverlapType("within open program");
                           copyProgramEntryExit.setOverlapEntryDate(entryDate2);
                           copyProgramEntryExit.setOverlapExitDate(exitDate);
                        }
                        else
                        {
                           programEntryExit.setOverlapID(programEntryExit2.getRowID());
                           programEntryExit.setOverlapType("within open program");
                           programEntryExit.setOverlapEntryDate(entryDate2);
                           programEntryExit.setOverlapExitDate(exitDate);
                        }
                     }
                  }
               }
               
               if (exitDate == null && exitDate2 != null)
               {
                  //program2 exit date is within currently running program1 - overlap
                  if (exitDate2.after(entryDate) || exitDate2.equals(entryDate))
                  {
                     //overlap established
                     //check where overlap begins, entryDate2 or entryDate
                     if (entryDate2.after(entryDate) || entryDate2.equals(entryDate))
                     {
                        //check for existing overlap
                        if (programEntryExit.getOverlapID() != -1)
                        {
                           //create copy
                           clients.get(i).addCopyProgramEntryExit(
                                 programEntryExit.getRowID(),
                                 programEntryExit.getEntryDate(),
                                 programEntryExit.getExitDate());
                           
                           int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                           ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                           copyProgramEntryExit.setCopy(true);
                           
                           copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                           copyProgramEntryExit.setOverlapType("program within this open program");
                           copyProgramEntryExit.setOverlapEntryDate(entryDate2);
                           copyProgramEntryExit.setOverlapExitDate(exitDate2);
                        }
                        else
                        {
                           programEntryExit.setOverlapID(programEntryExit2.getRowID());
                           programEntryExit.setOverlapType("program within this open program");
                           programEntryExit.setOverlapEntryDate(entryDate2);
                           programEntryExit.setOverlapExitDate(exitDate2);
                        }
                     }
                     else if (entryDate2.before(entryDate))
                     {
                        //check for existing overlap
                        if (programEntryExit.getOverlapID() != -1)
                        {
                           //create copy
                           clients.get(i).addCopyProgramEntryExit(
                                 programEntryExit.getRowID(),
                                 programEntryExit.getEntryDate(),
                                 programEntryExit.getExitDate());
                           
                           int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                           ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                           copyProgramEntryExit.setCopy(true);
                           
                           copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                           copyProgramEntryExit.setOverlapType("program within this open program");
                           copyProgramEntryExit.setOverlapEntryDate(entryDate);
                           copyProgramEntryExit.setOverlapExitDate(exitDate2);
                        }
                        else
                        {
                           programEntryExit.setOverlapID(programEntryExit2.getRowID());
                           programEntryExit.setOverlapType("program within this open program");
                           programEntryExit.setOverlapEntryDate(entryDate);
                           programEntryExit.setOverlapExitDate(exitDate2);
                        }
                     }
                  }
               }
               
               if (exitDate != null && exitDate2 != null)
               {
                  //program1 exit date is within program2
                  if (exitDate.after(entryDate2) || exitDate.equals(entryDate2))
                  {
                     if (exitDate.before(exitDate2) || exitDate.equals(exitDate2))
                     {
                        //overlap established
                        //check where overlap begins, entryDate or entryDate2
                        if (entryDate.after(entryDate2) || entryDate.equals(entryDate2))
                        {
                           //check for existing overlap
                           if (programEntryExit.getOverlapID() != -1)
                           {
                              //create copy
                              clients.get(i).addCopyProgramEntryExit(
                                    programEntryExit.getRowID(),
                                    programEntryExit.getEntryDate(),
                                    programEntryExit.getExitDate());
                              
                              
                              int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                              ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                              copyProgramEntryExit.setCopy(true);
                              
                              copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                              copyProgramEntryExit.setOverlapType("overlap");
                              copyProgramEntryExit.setOverlapEntryDate(entryDate);
                              copyProgramEntryExit.setOverlapExitDate(exitDate);
                           }
                           else
                           {
                              programEntryExit.setOverlapID(programEntryExit2.getRowID());
                              programEntryExit.setOverlapType("overlap");
                              programEntryExit.setOverlapEntryDate(entryDate);
                              programEntryExit.setOverlapExitDate(exitDate);
                           }
                        }
                        else if (entryDate.before(entryDate2))
                        {
                           //check for existing overlap
                           if (programEntryExit.getOverlapID() != -1)
                           {
                              //create copy
                              clients.get(i).addCopyProgramEntryExit(
                                    programEntryExit.getRowID(),
                                    programEntryExit.getEntryDate(),
                                    programEntryExit.getExitDate());
                              
                              
                              int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                              ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                              copyProgramEntryExit.setCopy(true);
                              
                              copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                              copyProgramEntryExit.setOverlapType("overlap");
                              copyProgramEntryExit.setOverlapEntryDate(entryDate2);
                              copyProgramEntryExit.setOverlapExitDate(exitDate);
                           }
                           else
                           {
                              programEntryExit.setOverlapID(programEntryExit2.getRowID());
                              programEntryExit.setOverlapType("overlap");
                              programEntryExit.setOverlapEntryDate(entryDate2);
                              programEntryExit.setOverlapExitDate(exitDate);
                           }
                        }
                     }
                  }
                  
                  //program2 exit date is within program1
                  if (exitDate2.after(entryDate) || exitDate2.equals(entryDate))
                  {
                     if (exitDate2.before(exitDate) || exitDate2.equals(exitDate))
                     {
                        //overlap established
                        //check where overlap begins, entryDate2 or entryDate
                        if (entryDate2.after(entryDate) || entryDate2.equals(entryDate))
                        {
                           //check for existing overlap
                           if (programEntryExit.getOverlapID() != -1)
                           {
                              //create copy
                              clients.get(i).addCopyProgramEntryExit(
                                    programEntryExit.getRowID(),
                                    programEntryExit.getEntryDate(),
                                    programEntryExit.getExitDate());
                              
                              
                              int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                              ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                              copyProgramEntryExit.setCopy(true);
                              
                              copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                              copyProgramEntryExit.setOverlapType("overlap");
                              copyProgramEntryExit.setOverlapEntryDate(entryDate2);
                              copyProgramEntryExit.setOverlapExitDate(exitDate2);
                           }
                           else
                           {
                              programEntryExit.setOverlapID(programEntryExit2.getRowID());
                              programEntryExit.setOverlapType("overlap");
                              programEntryExit.setOverlapEntryDate(entryDate2);
                              programEntryExit.setOverlapExitDate(exitDate2);
                           }
                        }
                        else if (entryDate2.before(entryDate))
                        {
                           //check for existing overlap
                           if (programEntryExit.getOverlapID() != -1)
                           {
                              //create copy
                              clients.get(i).addCopyProgramEntryExit(
                                    programEntryExit.getRowID(),
                                    programEntryExit.getEntryDate(),
                                    programEntryExit.getExitDate());
                              
                              
                              int copyIndex = clients.get(i).getCopyProgramEntryExits().size() - 1;
                              ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(copyIndex);
                              copyProgramEntryExit.setCopy(true);
                              
                              copyProgramEntryExit.setOverlapID(programEntryExit2.getRowID());
                              copyProgramEntryExit.setOverlapType("overlap");
                              copyProgramEntryExit.setOverlapEntryDate(entryDate);
                              copyProgramEntryExit.setOverlapExitDate(exitDate2);
                           }
                           else
                           {
                              programEntryExit.setOverlapID(programEntryExit2.getRowID());
                              programEntryExit.setOverlapType("overlap");
                              programEntryExit.setOverlapEntryDate(entryDate);
                              programEntryExit.setOverlapExitDate(exitDate2);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
   
   //sort client arraylist based on clientID, mostly used for presentation in output CSV
   public void sortClients()
   {
      Comparator<Client> cmp = new Comparator<Client>()
      {
         @Override
         public int compare(Client o1, Client o2)
         {
            return o1.getClientID() - o2.getClientID();
         }
      };
      
      Collections.sort(clients, cmp);
   }
   
   //returns position of client in the clients arraylist (-1 if not found)
   public int findClient(int clientID)
   {
      for(int i = 0; i < clients.size(); i++)
      {
         if(clients.get(i).getClientID() == clientID)
         {
            return i;
         }
      }
      
      return -1;
   }
   
   //test print function, not updated
   public void testPrintProgramEntryExits()
   {
      System.out.println("Client ID\tEntry Date\tExit Date");
      
      for (int i = 0; i < clients.size(); i++)
      {
         for (int j = 0; j < clients.get(i).getProgramEntryExits().size(); j++)
         {
            System.out.print(clients.get(i).getClientID());
            System.out.print("\t");
            
            SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
            
            System.out.print(f.format(clients.get(i).getProgramEntryExits().get(j).getEntryDate()));
            System.out.print("\t");
            if(clients.get(i).getProgramEntryExits().get(j).getExitDate() != null)
            {
               System.out.println(f.format(clients.get(i).getProgramEntryExits().get(j).getExitDate()));
            }
            else
            {
               System.out.println();
            }
         }
      }
   }
   
   //creates output file, will fail if old output file (or input file) is currently open in excel
   public void createOutputFileOverlap()
   {
      //delete old output csv
      File outputFile = new File("outputOverlap.csv");
      outputFile.delete(); //will fail if open in excel
      
      //will fail if open in excel
      //output (true activates append mode)
      try (FileWriter fw = new FileWriter("outputOverlap.csv", true);
            BufferedWriter bw = new BufferedWriter(fw); //buffered writer provides efficient output
            PrintWriter pw = new PrintWriter(bw)) //print writer provides easier syntax
      {
         //column headers
         pw.println("Type,Client ID,Entry Date,Exit Date,Row ID,Overlaps With Row,Overlap Type,Overlap Entry Date,Overlap Exit Date");
         
         for (int i = 0; i < clients.size(); i++)
         {
            //output original rows
            for (int j = 0; j < clients.get(i).getProgramEntryExits().size(); j++)
            {
               ProgramEntryExit programEntryExit = clients.get(i).getProgramEntryExits().get(j);
               
               //type
               if (programEntryExit.isCopy())
               {
                  pw.print("copy");
               }
               else
               {
                  pw.print("original");
               }
               
               //client ID column
               pw.print("," + clients.get(i).getClientID());
               
               //format dates to look good
               SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
               String entryDateStr = f.format(programEntryExit.getEntryDate());
               String exitDateStr = "";
               //must check for null exitDate to prevent error (null exit date == currently open program)
               if (programEntryExit.getExitDate() != null)
               {
                  exitDateStr = f.format(programEntryExit.getExitDate());
               }
               
               pw.print("," + entryDateStr + "," + exitDateStr);
               
               //rowID
               pw.print("," + programEntryExit.getRowID());
               
               //overlapID and overlapType columns
               if (programEntryExit.getOverlapID() != -1) //-1 overlapID indicates no overlap
               {
                  pw.print("," + programEntryExit.getOverlapID());
                  pw.print("," + programEntryExit.getOverlapType());
                  
                  entryDateStr = "";
                  exitDateStr = "";
                  if (programEntryExit.getOverlapEntryDate() != null)
                  {
                     entryDateStr = f.format(programEntryExit.getOverlapEntryDate());
                  }
                  if (programEntryExit.getOverlapExitDate() != null)
                  {
                     exitDateStr = f.format(programEntryExit.getOverlapExitDate());
                  }
                  pw.print("," + entryDateStr + "," + exitDateStr);
               }
               
               pw.println();
            }
            
            //output copy rows
            for (int j = 0; j < clients.get(i).getCopyProgramEntryExits().size(); j++)
            {
               ProgramEntryExit copyProgramEntryExit = clients.get(i).getCopyProgramEntryExits().get(j);
               
               //type
               if (copyProgramEntryExit.isCopy())
               {
                  pw.print("copy");
               }
               else
               {
                  pw.print("original");
               }
               
               //client ID column
               pw.print("," + clients.get(i).getClientID());
               
               //format dates to look good
               SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
               String entryDateStr = f.format(copyProgramEntryExit.getEntryDate());
               String exitDateStr = "";
               if (copyProgramEntryExit.getExitDate() != null)
               {
                  exitDateStr = f.format(copyProgramEntryExit.getExitDate());
               }
               
               pw.print("," + entryDateStr + "," + exitDateStr);
               
               //rowID
               pw.print("," + copyProgramEntryExit.getRowID());
               
               //overlapID and overlapType columns
               if (copyProgramEntryExit.getOverlapID() != -1)
               {
                  pw.print("," + copyProgramEntryExit.getOverlapID());
                  pw.print("," + copyProgramEntryExit.getOverlapType());
                  
                  entryDateStr = "";
                  exitDateStr = "";
                  if (copyProgramEntryExit.getOverlapEntryDate() != null)
                  {
                     entryDateStr = f.format(copyProgramEntryExit.getOverlapEntryDate());
                  }
                  if (copyProgramEntryExit.getOverlapExitDate() != null)
                  {
                     exitDateStr = f.format(copyProgramEntryExit.getOverlapExitDate());
                  }
                  pw.print("," + entryDateStr + "," + exitDateStr);
               }
               
               pw.println();
            }
         }
      } catch (IOException e)
      {
      }
   }
}

class Client
{
   private int clientID;
   private ArrayList<ServiceDate> serviceDates;
   private ArrayList<ProgramEntryExit> programEntryExits;
   private ArrayList<ProgramEntryExit> copyProgramEntryExits; //used to track multiple overlaps for a single row
   
   //constructors
   public Client(int clientID)
   {
      this.clientID = clientID;
      serviceDates = new ArrayList<ServiceDate>();
      programEntryExits = new ArrayList<ProgramEntryExit>();
      copyProgramEntryExits = new ArrayList<ProgramEntryExit>();
   }
   public Client(String clientIDStr)
   {
      clientID = Integer.parseInt(clientIDStr);
      serviceDates = new ArrayList<ServiceDate>();
      programEntryExits = new ArrayList<ProgramEntryExit>();
      copyProgramEntryExits = new ArrayList<ProgramEntryExit>();
   }
   
   //add ServiceDate or ProgramEntryExit for the client
   public void addServiceDate(String serviceDateStr)
   {
      serviceDates.add(new ServiceDate(serviceDateStr));
   }
   public void addProgramEntryExit(int rowID, String entryDateStr, String exitDateStr)
   {
      programEntryExits.add(new ProgramEntryExit(rowID, entryDateStr, exitDateStr));
   }
   public void addCopyProgramEntryExit(int rowID, Date entryDateStr, Date exitDateStr)
   {
      copyProgramEntryExits.add(new ProgramEntryExit(rowID, entryDateStr, exitDateStr));
   }

   //getter/setter
   public int getClientID()
   {
      return clientID;
   }
   public void setClientID(int clientID)
   {
      this.clientID = clientID;
   }
   public ArrayList<ServiceDate> getServiceDates()
   {
      return serviceDates;
   }
   public void setServiceDates(ArrayList<ServiceDate> serviceDates)
   {
      this.serviceDates = serviceDates;
   }
   public ArrayList<ProgramEntryExit> getProgramEntryExits()
   {
      return programEntryExits;
   }
   public void setProgramEntryExits(ArrayList<ProgramEntryExit> programEntryExits)
   {
      this.programEntryExits = programEntryExits;
   }
   public ArrayList<ProgramEntryExit> getCopyProgramEntryExits()
   {
      return copyProgramEntryExits;
   }
   public void setCopyProgramEntryExits(ArrayList<ProgramEntryExit> copyProgramEntryExits)
   {
      this.copyProgramEntryExits = copyProgramEntryExits;
   }
}

//holds the date a service was carried out for a client and if that date passes (used in another program)
class ServiceDate
{
   private Date serviceDate = null;
   //passing indicates that a serviceDate falls within a ProgramEntryExit date range
   private boolean passing;
   
   //constructor
   public ServiceDate(String serviceDateStr)
   {
      try
      {
         if(serviceDateStr != "")
         {
            serviceDate = new SimpleDateFormat("MM/dd/yyyy").parse(serviceDateStr);
         }
      } catch (ParseException e)
      {
         e.printStackTrace();
      }
      passing = false;
   }
   
   //getter/setter
   public Date getServiceDate()
   {
      return serviceDate;
   }
   public boolean isPassing()
   {
      return passing;
   }
   public void setServiceDate(Date serviceDate)
   {
      this.serviceDate = serviceDate;
   }
   public void setPassing(boolean passing)
   {
      this.passing = passing;
   }
}

//holds a date range for which a client entered and exited a program
class ProgramEntryExit
{
   private Date entryDate = null;
   private Date exitDate = null;
   
   private int rowID;
   private boolean isCopy = false; //copy indicates that this is at least the 2nd overlap occurring for a particular rowID
   
   private int overlapID = -1; //indicates which rowID overlapping with
   private String overlapType = null;
   private Date overlapEntryDate = null; //date that overlap starts occurring
   private Date overlapExitDate = null; //date that overlap ends
   
   //constructor
   public ProgramEntryExit(int rowID, String entryDateStr, String exitDateStr)
   {
      this.rowID = rowID;
      
      try
      {
         //blank csv cells for date will be equal to an empty string "" (not null)
         if(entryDateStr != "")
         {
            entryDate = new SimpleDateFormat("MM/dd/yyyy").parse(entryDateStr);
         }
         if(exitDateStr != "")
         {
            exitDate = new SimpleDateFormat("MM/dd/yyyy").parse(exitDateStr);
         }
      }
      catch (ParseException e)
      {
         e.printStackTrace();
      }
   }
   
   //constructor for copy
   public ProgramEntryExit(int rowID, Date entryDate, Date exitDate)
   {
      this.rowID = rowID;
      this.entryDate = entryDate;
      this.exitDate = exitDate;
   }
   
   //getter/setter
   public Date getEntryDate()
   {
      return entryDate;
   }
   public Date getExitDate()
   {
      return exitDate;
   }
   public int getOverlapID()
   {
      return overlapID;
   }
   public void setEntryDate(Date entryDate)
   {
      this.entryDate = entryDate;
   }
   public void setExitDate(Date exitDate)
   {
      this.exitDate = exitDate;
   }
   public void setOverlapID(int overlapID)
   {
      this.overlapID = overlapID;
   }
   public String getOverlapType()
   {
      return overlapType;
   }
   public void setOverlapType(String overlapType)
   {
      this.overlapType = overlapType;
   }
   public int getRowID()
   {
      return rowID;
   }
   public boolean isCopy()
   {
      return isCopy;
   }
   public Date getOverlapEntryDate()
   {
      return overlapEntryDate;
   }
   public Date getOverlapExitDate()
   {
      return overlapExitDate;
   }
   public void setRowID(int rowID)
   {
      this.rowID = rowID;
   }
   public void setCopy(boolean isCopy)
   {
      this.isCopy = isCopy;
   }
   public void setOverlapEntryDate(Date overlapEntryDate)
   {
      this.overlapEntryDate = overlapEntryDate;
   }
   public void setOverlapExitDate(Date overlapExitDate)
   {
      this.overlapExitDate = overlapExitDate;
   }
}