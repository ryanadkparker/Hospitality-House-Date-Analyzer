import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.opencsv.CSVReader;

public class CSVSearchClient
{
   public static void main(String[] args)
   {
      ClientsHandler clientsHandler = new ClientsHandler();
      clientsHandler.createOutputFile();
   }
}

class ClientsHandler
{
   private String filePathProgramEntryExits = "ProgramEntryExits.csv";
   private String filePathServices = "services.csv";
   private ArrayList<Client> clients;
   
   //columns
   private int entryExitClientIDCol;
   private int entryDateCol;
   private int exitDateCol;
   
   private int serviceClientIDCol;
   private int serviceStartDateCol;
   
   //constructor
   public ClientsHandler()
   {
      clients = new ArrayList<Client>();
      List<String[]> listProgramEntryExits = null;
      List<String[]> listServices = null;
      
      try
      {
         //read both CSV files into a list
         CSVReader csvReader = new CSVReader(new FileReader(new File(filePathProgramEntryExits)));
         listProgramEntryExits = csvReader.readAll();
         csvReader = new CSVReader(new FileReader(new File(filePathServices)));
         listServices = csvReader.readAll();
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
      //column headers for services CSV
      for(int i = 0; i < listServices.get(0).length; i++)
      {
         String columnHeader = listServices.get(0)[i];
         
         if(columnHeader.equals("Client ID"))
         {
            serviceClientIDCol = i;
         }
         if(columnHeader.equals("Service Start Date"))
         {
            serviceStartDateCol = i;
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
      Comparator<String[]> cmpServices = new Comparator<String[]>()
      {
         @Override
         public int compare(String[] o1, String[] o2)
         {
            return Integer.valueOf(o1[serviceClientIDCol]).compareTo(Integer.valueOf(o2[serviceClientIDCol]));
         }
      };
      
      //sort both lists by clientID, but skip first row (columnHeader row)
      Collections.sort(listProgramEntryExits.subList(1, listProgramEntryExits.size()), cmpProgramEntryExits);
      Collections.sort(listServices.subList(1, listServices.size()), cmpServices);
      
      //start populating list of clients from ProgramEntryExits
      int previousClientID = -1;
      int clientIndex = -1;
      for (int i = 1; i < listProgramEntryExits.size(); i++)
      {
         int clientID = Integer.parseInt(listProgramEntryExits.get(i)[entryExitClientIDCol]);
         
         if (previousClientID != clientID)
         {
            clients.add(new Client(clientID));
            clientIndex = clients.size() - 1;
         }
         clients.get(clientIndex).addProgramEntryExit(
               listProgramEntryExits.get(i)[entryDateCol],
               listProgramEntryExits.get(i)[exitDateCol]);
         
         previousClientID = clientID;
      }
      
      //assign serviceDates to clients (creates a new client if not found)
      previousClientID = -1;
      clientIndex = -1;
      for (int i = 1; i < listServices.size(); i++)
      {
         int clientID = Integer.parseInt(listServices.get(i)[serviceClientIDCol]);
         
         if (previousClientID != clientID)
         {
            clientIndex = findClient(clientID);
            if (clientIndex == -1) //client does not exist
            {
               clients.add(new Client(clientID));
               clientIndex = clients.size() - 1;
            }
         }
         clients.get(clientIndex).addServiceDate(
               listServices.get(i)[serviceStartDateCol]);
         
         previousClientID = clientID;
      }
      
      findPassingServiceDates();
      sortClients();
   }
   
   //checks if each service date falls within any program entry exits for a client
   public void findPassingServiceDates()
   {
      for (int i = 0; i < clients.size(); i++)
      {
         for (int j = 0; j < clients.get(i).getServiceDates().size(); j++)
         {
            ServiceDate serviceDate = clients.get(i).getServiceDates().get(j);
            
            for (int k = 0; k < clients.get(i).getProgramEntryExits().size(); k++)
            {
               //if serviceDate is found to be passing, don't search through remaining programEntryExits
               if (serviceDate.isPassing())
               {
                  break;
               }
               
               ProgramEntryExit programEntryExit = clients.get(i).getProgramEntryExits().get(k);
               
               if (serviceDate.getServiceDate().after(programEntryExit.getEntryDate())
                     && programEntryExit.getExitDate() == null)
               {
                  //no exit date
                  serviceDate.setPassing(true);
               }
               else if (serviceDate.getServiceDate().after(programEntryExit.getEntryDate())
                     && serviceDate.getServiceDate().before(programEntryExit.getExitDate()))
               {
                  //between
                  serviceDate.setPassing(true);
               }
               else if (serviceDate.getServiceDate().equals(programEntryExit.getEntryDate())
                     || serviceDate.getServiceDate().equals(programEntryExit.getExitDate()))
               {
                  //same as entry or exit date
                  serviceDate.setPassing(true);
               }
            }
         }
      }
   }
   
   //sort client arraylist based on clientID
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
   
   public void createOutputFile()
   {
      //delete old output.csv
      File outputFile = new File("output.csv");
      outputFile.delete(); //will fail if open in excel
      
      //will fail if open in excel
      //output (true activates append mode)
      try (FileWriter fw = new FileWriter("output.csv", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw))
      {
         //column headers
         pw.println("Client ID,Service Start Date,pass/fail");
         
         for (int i = 0; i < clients.size(); i++)
         {
            for (int j = 0; j < clients.get(i).getServiceDates().size(); j++)
            {
               //client ID column
               pw.print(clients.get(i).getClientID());
               
               //format serviceDate column to look good
               SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
               String serviceDateStr = f.format(clients.get(i).getServiceDates().get(j).getServiceDate());
               pw.print("," + serviceDateStr);
               
               //pass/fail column
               if (clients.get(i).getServiceDates().get(j).isPassing())
               {
                  pw.println("," + "pass");
               }
               else
               {
                  pw.println("," + "fail");
               }
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
   
   //constructors
   public Client(int clientID)
   {
      this.clientID = clientID;
      serviceDates = new ArrayList<ServiceDate>();
      programEntryExits = new ArrayList<ProgramEntryExit>();
   }
   public Client(String clientIDStr)
   {
      clientID = Integer.parseInt(clientIDStr);
      serviceDates = new ArrayList<ServiceDate>();
      programEntryExits = new ArrayList<ProgramEntryExit>();
   }
   
   //add ServiceDate or ProgramEntryExit for the client
   public void addServiceDate(String serviceDateStr)
   {
      serviceDates.add(new ServiceDate(serviceDateStr));
   }
   public void addProgramEntryExit(String entryDateStr, String exitDateStr)
   {
      programEntryExits.add(new ProgramEntryExit(entryDateStr, exitDateStr));
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
}

//holds the date a service was carried out for a client and if that date passes
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
   
   //constructor
   public ProgramEntryExit(String entryDateStr, String exitDateStr)
   {
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
   
   //getter/setter
   public Date getEntryDate()
   {
      return entryDate;
   }
   public Date getExitDate()
   {
      return exitDate;
   }
   public void setEntryDate(Date entryDate)
   {
      this.entryDate = entryDate;
   }
   public void setExitDate(Date exitDate)
   {
      this.exitDate = exitDate;
   }
}