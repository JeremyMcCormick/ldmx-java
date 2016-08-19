package org.ldmx.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser; 

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * A class to convert lhe events to Stdhep.
 * 
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a> 
 */
public class LheToStdhep {
    
    private static final int N_PARTICLE_INDEX = 0;
    private static final int EVENT_NUMBER_INDEX = 1;
    
    private static final int PDG_ID_INDEX = 1;
    private static final int STATUS_INDEX = 2; 
    private static final int FIRST_MOTHER_INDEX = 3; 
    private static final int SECOND_MOTHER_INDEX = 4; 
    private static final int FIRST_DAUGHTER_INDEX = 5; 
    private static final int SECOND_DAUGHTER_INDEX = 6;
    
    private static double targetThickness = 0.35; // mm
    private static double targetDeltaX = 10.0; // mm
    private static double targetDeltaY = 20.0; // mm
    private static double targetZPosition  = 0.00; // mm 
    
    
    public static void main(String[] args) throws IOException {

        
        String lheGzFileName = null; 
        String stdhepFileName = "output.stdhep";

        CommandLineParser parser = new DefaultParser();

        // Create the Options
        // TODO: Add ability to parse list of files.
        Options options = new Options(); 
        options.addOption("i", "input",  true, "Input lhe.gz file name");
        options.addOption("o", "output", true, "Output Stdhep file name");
       
        try {
           
            // Parse the command line arguments
            CommandLine line = parser.parse(options, args);
            
            // If the file is not specified, notify the user and exit the 
            // application.
            if(!line.hasOption("i")){
                System.out.println("Please specify an LHE file to process.");
                System.exit(0);
            }
           
            // Get the name of the input file
            lheGzFileName = line.getOptionValue("i");
       
            // If the user specified an output file name, use that instead of
            // the default.
            if(line.hasOption("o")){
                stdhepFileName = line.getOptionValue("o");
            } else { 
                stdhepFileName = lheGzFileName.substring(lheGzFileName.lastIndexOf("/") + 1, lheGzFileName.indexOf(".lhe.gz"));
                stdhepFileName += ".stdhep";
            }
            
        } catch(ParseException e){
            System.out.println("Unable to parse command line arguments: " + e.getMessage());
        }
        
        GZIPInputStream lheGzStream = new GZIPInputStream(new FileInputStream(lheGzFileName));
        
        List<Element> events = getLheEvents(lheGzStream);
        System.out.println("[ LheToStdhep ] : A total of " + events.size() + " will be processed.");
        System.out.println("[ LheToStdhep ] : Events will be written to file: " + stdhepFileName);
        
        convertToStdHep(events, stdhepFileName);
    }

    /**
     * 
     */
    static private void convertToStdHep(List<Element> events, String stdhepFileName) throws IOException{
       
        StdhepWriter writer = new StdhepWriter(stdhepFileName, "Import Stdhep Events", "Imported from LHE generated from MadGraph", events.size());
        writer.setCompatibilityMode(false);
        
        for(Element event : events){
            writeEvent(event, writer);
        }
        writer.close();
    }

    /**
     * 
     */
    private static List<Element> getLheEvents(GZIPInputStream lheGzStream) {
        
        // Instantiate the SAX parser used to build the JDOM document
        SAXBuilder builder = new SAXBuilder(); 
        
        // Parse the lhe file and build the JDOM document
        Document document = null;
        List<Element> eventNodes = null; 
        try {
            
            document = (Document) builder.build(lheGzStream);
            
            // Get the root node
            Element rootNode = document.getRootElement(); 
            
            // Get a list of all nodes of type event
            eventNodes = rootNode.getChildren("event");
        
        } catch (JDOMException e) {
            e.printStackTrace();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return eventNodes; 
    }
   
    /** 
     * 
     */
    private static Hep3Vector getVertexPosition() { 
       
       double x = ThreadLocalRandom.current().nextDouble(-targetDeltaX, targetDeltaX);
       double y = ThreadLocalRandom.current().nextDouble(-targetDeltaY, targetDeltaY);
       double z = ThreadLocalRandom.current().nextDouble(
               targetZPosition - targetThickness/2, 
               targetZPosition + targetThickness/2);
        
       return new BasicHep3Vector(x, y, z); 
    }

    /**
     * 
     */
    private static void writeEvent(Element event, StdhepWriter writer) throws IOException{
        
        int eventNumber = 0;
        int numberOfParticles = 0; 
        int particleIndex = 0;
        int pdgID[] = null; 
        int particleStatus[] = null;
        int motherParticles[] = null; 
        int daughterParticles[] = null; 
        double particleMomentum[] = null;
        double particleVertex[] = null; 
        
        // Get the text within the event element node.  An element node contains
        // information describing the event and it's particles.  The PDG ID of
        // a particle along with it's kinematics are listed on it's own line.
        // In order to parse the information for each particle, the text is 
        // split using the newline character as a delimiter.  
        String[] eventData = event.getTextTrim().split("\n");   
     
        // Get the vertex position that will be used for this particle.
        Hep3Vector vertexPosition = getVertexPosition();
        
        for (int datumIndex = 0; datumIndex < eventData.length; datumIndex++) {
            
            // Split a line by whitespace
            String[] eventTokens = eventData[datumIndex].split("\\s+");
        
            if(datumIndex == 0) {
                
                eventNumber = Integer.valueOf(eventTokens[EVENT_NUMBER_INDEX]);
                System.out.println("#================================================#\n#");
                System.out.println("# Event: " + eventNumber);
                
                numberOfParticles = Integer.valueOf(eventTokens[N_PARTICLE_INDEX]);
                System.out.println("# Number of particles: " + numberOfParticles + "\n#");
                System.out.println("#================================================#");
        
                // Reset all arrays used to build the Stdhep event
                particleIndex = 0; 
                particleStatus = new int[numberOfParticles];
                pdgID = new int[numberOfParticles];
                motherParticles = new int[numberOfParticles*2];
                daughterParticles = new int[numberOfParticles*2];
                particleMomentum = new double[numberOfParticles*5];
                particleVertex = new double[numberOfParticles*4];
            
                continue;
            }
    
            // Get the PDG ID of the particle
            pdgID[particleIndex] = Integer.valueOf(eventTokens[PDG_ID_INDEX]);
            
            
            System.out.println(">>> PDG ID: " + pdgID[particleIndex]);
            
            // Get the status of the particle (initial state = -1, final state = 1, resonance = 2)
            particleStatus[particleIndex] = Integer.valueOf(eventTokens[STATUS_INDEX]);
            if(particleStatus[particleIndex] == -1) particleStatus[particleIndex] = 3; 
            System.out.println(">>>> Particle Status: " + particleStatus[particleIndex]);
            
            motherParticles[particleIndex*2] = Integer.valueOf(eventTokens[FIRST_MOTHER_INDEX]);
            motherParticles[particleIndex*2 + 1] = Integer.valueOf(eventTokens[SECOND_MOTHER_INDEX]);
            System.out.println(">>>> Mothers: 1) " + motherParticles[particleIndex*2] + " 2) " + motherParticles[particleIndex*2 + 1]);
            
            // Get the daughter particles
            daughterParticles[particleIndex*2] = Integer.valueOf(eventTokens[FIRST_DAUGHTER_INDEX]);
            daughterParticles[particleIndex*2 + 1] = Integer.valueOf(eventTokens[SECOND_DAUGHTER_INDEX]);
            if (daughterParticles[particleIndex*2] != 0 || daughterParticles[particleIndex*2 + 1] != 0) throw new RuntimeException("wtf?");
            System.out.println(">>>> Daughter: 1) " + daughterParticles[particleIndex*2] + " 2) " + daughterParticles[particleIndex*2 + 1]);
            
            // Get the kinematics of the particle
            particleMomentum[particleIndex*5] = Double.valueOf(eventTokens[7]);     // px
            particleMomentum[particleIndex*5 + 1] = Double.valueOf(eventTokens[8]); // py   
            particleMomentum[particleIndex*5 + 2] = Double.valueOf(eventTokens[9]); // pz
            particleMomentum[particleIndex*5 + 3] = Double.valueOf(eventTokens[10]); // Particle Energy
            particleMomentum[particleIndex*5 + 4] = Double.valueOf(eventTokens[11]); // Particle Mass
            System.out.println(">>>> px: " + particleMomentum[particleIndex*5] 
                    + " py: " + particleMomentum[particleIndex*5 + 1]
                    + " pz: " + particleMomentum[particleIndex*5 + 2]
                    + " energy: " + particleMomentum[particleIndex*5 + 3]
                    + " mass: " + particleMomentum[particleIndex*5 + 4]
            );
            
            particleVertex[particleIndex*4] = vertexPosition.x();
            particleVertex[particleIndex*4+1] = vertexPosition.y();
            particleVertex[particleIndex*4+2] = vertexPosition.z();
            particleVertex[particleIndex*4+3] = 0; 
           
            System.out.println(">>>> vertex: " + vertexPosition.toString());
            
            // Increment the particle number
            particleIndex++;
            
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        }
        
        // Create the Stdhep event and write it 
        StdhepEvent stdhepEvent = new StdhepEvent(eventNumber, numberOfParticles, particleStatus, 
                pdgID, motherParticles, daughterParticles, particleMomentum, particleVertex);
        writer.writeRecord(stdhepEvent);
    }
}
