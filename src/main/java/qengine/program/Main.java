package qengine.program;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;

final class Main {
	
	
	static final String baseURI = null;
	static final String workingDir = "data/";
	static final String outputDir = "output/";

	static final String queryFile = workingDir + "13k.queryset";
	//static final String queryFile = workingDir + "6500.queryset";
	//static final String queryFile = workingDir + "2600.queryset";
	//static final String queryFile = workingDir + "STAR_ALL_workload.queryset";

	static final String dataFile = workingDir + "2M.nt";
	//static final String dataFile = workingDir + "1_9M.nt";
	//static final String dataFile = workingDir + "500k.nt";
	//static final String dataFile = workingDir + "100K.nt";
	
	//static final String outputFile = outputDir + "export_query_500K.csv";
	static final String outputFile = outputDir + "export_query_2M.csv";
	//static final String outputFileTime = outputDir + "output1_9M.csv";
	static final String outputFileTime = outputDir + "output2M.csv";
	
	
	private static List<Statement> statementList;
	private static Dictionary dictionary;
	private static Hexastore hexastore;
	private static BufferedWriter writer;
	private static BufferedWriter writer2;
	
	// Declarer une liste pour stocker les requetes
	static List<ParsedQuery> queriesList= new ArrayList<>();;
	
	
	private static int countNbReq = 1;
	private static int nbReponse = 0 ;
	private static List<Integer> tabReponse = new ArrayList<>();
	private static int duplicateCount = 0;
	private static int tripleCount = 0;
	
    private static long startTimeParseData;
    private static long endTimeParseData;
    private static long endTimeDictionary;
    private static long endTimeHexastore;
    private static long endTimeparseQueries; 
    private static long endTimeprocessAQuery;

	// ========================================================================

    public static void processAQuery(ParsedQuery query, Model rdfModel) throws IOException {
        List<StatementPattern> patterns = StatementPatternCollector.process(query.getTupleExpr());

        String predicate = null;
        String object = null;
        int keyPredicate = -1;
        int keyObject = -1;
        List<Integer> result = new ArrayList<>();
        int i = 0;
        for (StatementPattern pattern : patterns) {
            System.out.println("\nPredicat du pattern :" + pattern.getPredicateVar().getValue());
            System.out.println("Object du pattern :" + pattern.getObjectVar().getValue());
            writer.write("Predicat du pattern :" + pattern.getPredicateVar().getValue());
            writer.write("\nObject du pattern :" + pattern.getObjectVar().getValue());

            predicate = pattern.getPredicateVar().getValue().toString();
            object = pattern.getObjectVar().getValue().toString();
            keyPredicate = dictionary.getKey(predicate);
            keyObject = dictionary.getKey(object);
            if (keyPredicate == -1 || keyObject == -1) {
                System.out.println("Predicate ou Object inexistante");
                writer.write("Predicate ou Object inexistantes\n");
                return;
            }
            if (i == 0) {
                result.addAll(hexastore.getSubject("ops", keyPredicate, keyObject));
            } else {
                result.retainAll(hexastore.getSubject("ops", keyPredicate, keyObject));
            }
            i += 1;
        }

        writer.write("\nLa liste des reponses:\n");

        if (result.size() == 0) {
            System.out.println("\nPas de resultat trouve pour cette requete.");
           writer.write("Pas de resultat trouve pour cette requete.\n");
        }
        nbReponse = result.size();
        for (int cle : result) {
            System.out.println("--> key :" + cle + ", Value :" + dictionary.getValue(cle));
            writer.write("key :" + cle + ", Value :" + dictionary.getValue(cle) + "\n");
        }
        writer.write("le nombre de reponse: " + nbReponse + "\n");

        System.out.println("--------------------------------------------------");
        writer.write("--------------------------------------------------------\n");
    }
	

	public static void main(String[] args) throws Exception {		
		startTimeParseData = System.currentTimeMillis();
		parseData();
		endTimeParseData = System.currentTimeMillis();
		
		dictionary = new Dictionary(statementList);
		dictionary.createDictionary();

		endTimeDictionary = System.currentTimeMillis();
		
		hexastore = new Hexastore(statementList, dictionary);
		hexastore.creationIndexHexastore();
		endTimeHexastore = System.currentTimeMillis();
		
		writer = new BufferedWriter(new FileWriter(outputFile));
		writer2 = new BufferedWriter(new FileWriter(outputFileTime));
		
		parseQueries();
		endTimeparseQueries = System.currentTimeMillis();
		
		// Charger le modele RDF a�partir du fichier N-Triples
	    Model rdfModel = ModelFactory.createDefaultModel();
	    FileManager.get().readModel(rdfModel, dataFile, null, "N-TRIPLE");

	    for (ParsedQuery query : queriesList) {
	        System.out.println("la requete " + countNbReq + ":");
	        writer.write("la requete " + countNbReq + ":\n");
	        processAQuery(query, rdfModel); // Passez le modele RDF a la methode
	        tabReponse.add(nbReponse);
	        countNbReq++;
	    }
		
	    endTimeprocessAQuery = System.currentTimeMillis();
	    System.out.println("Nombre total de doublons dans les requetes : "+duplicateCount+"\n");
        
	    // Mesurer le temps de lecture des donnees
        long dataReadTime = endTimeParseData - startTimeParseData;
        // Mesurer le temps de Creer le dictionnaire
        long dictionaryCreationTime =  endTimeDictionary - endTimeParseData;
        // Mesurer le temps de Creer l'index
        long hexastoreCreationTime =  endTimeHexastore - endTimeDictionary;
        // Mesurer le temps de lecture des requetes
        long queryReadTime = endTimeparseQueries - endTimeHexastore;
        // Mesurer le temps total d'evaluation du workload
        long totalEvaluationTime = endTimeprocessAQuery - endTimeparseQueries;
        // Mesurer le temps total du programme (du debut a la fin)
        long totalTime = System.currentTimeMillis() - startTimeParseData;
        
        // Afficher les resultats dans le terminal
        System.out.println("Nombre de triplets RDF : " + tripleCount);
        System.out.println("Nombre de requetes : " + (countNbReq-1));
        System.out.println("Temps de lecture des donnees : " + dataReadTime + " ms");
        System.out.println("Temps de lecture des requetes : " + queryReadTime + " ms");
        System.out.println("Temps de creation du dictionnaire : " + dictionaryCreationTime + " ms");
        System.out.println("Temps de creation des index : " + hexastoreCreationTime + " ms");
        System.out.println("Temps total d'evaluation du workload : " + totalEvaluationTime + " ms");
        System.out.println("Temps total (du debut a la fin du programme) : " + totalTime + " ms");
        
        //sauvgarder ces infos dans le fichier output.csv
		writer2.write("Nom du fichier de donnees: "+dataFile +"\n");
		writer2.write("Nom du dossier des requetes: "+queryFile +"\n");
		writer2.write("Nombre de triplets RDF: "+ tripleCount +"\n");
		writer2.write("Nombre de requetes: "+(countNbReq-1) +"\n");
		writer2.write("Temps de lecture des donnees: "+dataReadTime +"\n");
		writer2.write("Temps de lecture des requetes: "+queryReadTime +"\n");
		writer2.write("Temps de creation du dictionnaire: "+dictionaryCreationTime +"\n");
		writer2.write("Temps de creation des index: "+hexastoreCreationTime +"\n");
		writer2.write("Temps total d'evaluation du workload: "+totalEvaluationTime +"\n");
		writer2.write("Temps total (du debut a la fin du programme): "+totalTime +"\n");
		writer2.write("Nombre total de doublons dans les requetes : "+duplicateCount +"\n");
		writer.close();
        writer2.close();
        
      //Histogramme
        SwingUtilities.invokeLater(() -> {
            Histogramme example = new Histogramme("Histogramme pour "+(countNbReq-1)+" requetes sur "+tripleCount+" de donnees", tabReponse);
            example.setSize(800, 600);
            example.setLocationRelativeTo(null);
            example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            example.setVisible(true);
        });
        
	}  

	// ========================================================================
    
	private static void parseQueries() throws FileNotFoundException, IOException {
		try (Stream<String> lineStream = Files.lines(Paths.get(queryFile))) {
			SPARQLParser sparqlParser = new SPARQLParser();
			Iterator<String> lineIterator = lineStream.iterator();
			StringBuilder queryString = new StringBuilder();
	        Set<String> uniqueQueries = new HashSet<>();
			
			// On stocke plusieurs lignes jusqu'à ce que l'une d'entre elles se termine par un '}'On considère alors que c'est la fin d'une requête 
			while (lineIterator.hasNext()){  
				String line = lineIterator.next();
				queryString.append(line);
				if (line.trim().endsWith("}")) {
	                // Vérifier si la requête est déjà présente dans l'ensemble
	                if (!uniqueQueries.add(queryString.toString())) {
	                    duplicateCount++;
	                }
					ParsedQuery query = sparqlParser.parseQuery(queryString.toString(), baseURI);
	                // Stocker la requ�te dans la liste
	                queriesList.add(query);
					queryString.setLength(0);					
				}
			}
		}
	}

	private static void parseData() throws FileNotFoundException, IOException {

		try (Reader dataReader = new FileReader(dataFile)) {
			MainRDFHandler rdfHandler = new MainRDFHandler();
			// On va parser des données au format ntriples
			NTriplesParser rdfParser = new NTriplesParser();

			// On utilise notre implémentation de handler
			rdfParser.setRDFHandler(rdfHandler);

			// Parsing et traitement de chaque triple par le handler
			rdfParser.parse(dataReader, baseURI);
			statementList = rdfHandler.getStatementList();
			
	        // Compter le nombre de triples
	        tripleCount = rdfHandler.getTripleCount();
		}
	}
}
