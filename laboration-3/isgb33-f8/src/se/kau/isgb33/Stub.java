package se.kau.isgb33;

import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import javax.swing.*;
import java.awt.*;

public class Stub {

	public static void main(String[] args) {
		
		JFrame frame = new JFrame("Search By Genre");
		frame.setSize(400, 500);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		
		JTextArea searchDisplay = new JTextArea("");
		searchDisplay.setLineWrap(true);
		searchDisplay.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(searchDisplay);
		
		JPanel searchInputButtonPanel = new JPanel();
		searchInputButtonPanel.setLayout(new BorderLayout());
		
		JTextField searchInput = new JTextField();
		searchInputButtonPanel.add(searchInput, BorderLayout.CENTER);
		
		JButton searchButton = new JButton("Sök");
		searchInputButtonPanel.add(searchButton, BorderLayout.EAST);
		
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				/* Från F7 */
				String connString;
				
				try (InputStream input = new FileInputStream("connection.properties")) {

					Properties prop = new Properties();
					prop.load(input);
					connString = prop.getProperty("db.connection_string");

					ConnectionString connectionString = new ConnectionString(connString);
					MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
							.build();
					MongoClient mongoClient = MongoClients.create(settings);
					MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
					MongoCollection<Document> collection = database.getCollection("movies"); /* collection heter movies, som JSON-filen. */

					/* Tilldela variabeln genre användarens input i searchInput.
					 * Eftersom genre values är case sensitive så ska det väl så klart gå att skriva Drama och drama och få fram ett resultat. */
					String genreInput = searchInput.getText().toLowerCase();
					String genre = Character.toUpperCase(genreInput.charAt(0)) + genreInput.substring(1);
					
		            AggregateIterable<Document> myDocs = collection.aggregate(Arrays.asList(
		            		Aggregates.match(Filters.eq("genres", genre)), /* Filtrera efter genre */
		                    Aggregates.project(Projections.include("title", "year")),
		                    Aggregates.sort(Sorts.descending("title")), /* Från Z - A */
		                    Aggregates.limit(10)
		            ));
		            	
		            try (MongoCursor<Document> iterator = myDocs.iterator()) {
		            	
		            	/* Om användern gett en "ogiltig" genre */
		            	if(!iterator.hasNext()) {
		            		
		            		searchDisplay.setText("Ingen film matchade kategorin");
		            		return;
		            	}
						
		            	/* För att sammanslå både titel och år i en sträng.
		            	 * Istället för String.concat */
		            	StringBuilder resultBuilder = new StringBuilder();

		            	/* Intererar så länge det finns dokument
		            	 * Inom limit(10) i det här fallet */
		                while (iterator.hasNext()) {
		                	 
		                	Document myDoc = iterator.next();
		                    String title = myDoc.getString("title");

		                    /* För att hantera year. Year kunde tydligen vara både sträng och int. */
		                    if (myDoc.get("year") instanceof Integer) {

		                    	int year = myDoc.getInteger("year");
		                        resultBuilder.append(title).append(", ").append(year).append('\n');
		                    } else if (myDoc.get("year") instanceof String) {
		                    	
		                    	String year = myDoc.getString("year");
		                        resultBuilder.append(title).append(", ").append(year).append('\n');
		                    }
		                }

						 searchDisplay.setText(resultBuilder.toString());
					}
				} catch (FileNotFoundException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				} catch (IOException er) {
					// TODO Auto-generated catch block
					er.printStackTrace();
				}
	        }
		});
		
		frame.add(scrollPane, BorderLayout.CENTER);
		frame.add(searchInputButtonPanel, BorderLayout.SOUTH);
		frame.setVisible(true);
	}
}
