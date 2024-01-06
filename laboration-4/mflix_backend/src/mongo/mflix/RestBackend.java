package mongo.mflix;

import static spark.Spark.get;
import static spark.Spark.post;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.List;

import org.apache.commons.text.WordUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

public class RestBackend {

	public static void main(String[] args) throws IOException {
		
		  String connString;
		  Logger logger = LoggerFactory.getLogger(RestBackend.class);
		  InputStream input = new FileInputStream("connection.properties");
		  
		  Properties prop = new Properties();
		  prop.load(input);
		  connString = prop.getProperty("db.connection_string");
		  logger.info(connString);
		  
		  ConnectionString connectionString = new ConnectionString(connString);
		  MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString).build();
		  MongoClient mongoClient = MongoClients.create(settings);
		  MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
		  logger.info(prop.getProperty("db.name"));

		  /* TITLE */
	      get("/title/:title", (req,res)-> {
	    	  
		      MongoCollection<Document> collection = database.getCollection("movies");
		      String filter=req.params("title").toLowerCase();
		      filter=WordUtils.capitalizeFully(filter);
		      logger.info("Filtering movies for title: " + filter);
	    	  Document myDoc = collection.find(Filters.eq("title", filter)).first();
	    	  
	    	  if (myDoc != null) {
	    	      myDoc.remove("_id");
	    	      myDoc.remove("poster");
	    	      myDoc.remove("fullplot");
	    	      myDoc.remove("cast");
	    	      
	    	      return myDoc.toJson();
	    	  } else {
	    		  res.status(404);
	    		  return ("<html><body><h1>Title not found.</h1></body></html>");
	    	  }
	      });
	      
	      /* FULLPLOT */
	      get("/fullplot/:title", (req,res)-> {
	    	  
		      MongoCollection<Document> collection = database.getCollection("movies");
		      String filter=req.params("title").toLowerCase();
		      filter=WordUtils.capitalizeFully(filter);
		      logger.info("Finding plot for: " + filter);
	    	  Document myDoc = collection.find(Filters.eq("title", filter)).first();
	    	  
	    	  if (myDoc != null) {
	    		  String title = myDoc.getString("title");
	    		  String fullplot = myDoc.getString("fullplot");

		          return new Document("title", title).append("fullplot", fullplot).toJson();
	    	  } else {
	    		  res.status(404);
	    		  return ("<html><body><h1>Title not found.</h1></body></html>");
	    	  }
	      });

	      /* CAST */
	      get("/cast/:title", (req,res)-> {
	    	  
	    	  MongoCollection<Document> collection = database.getCollection("movies");
		      String filter=req.params("title").toLowerCase();
		      filter=WordUtils.capitalizeFully(filter);
		      logger.info("Finding cast for: " + filter);
	    	  Document myDoc = collection.find(Filters.eq("title", filter)).first();
	    	  
	    	  if (myDoc != null) {
	    		  String title = myDoc.getString("title");
	    		  List<String > cast = myDoc.getList("cast", String.class);

		          return new Document("title", title).append("cast", cast).toJson();
	    	  } else {
	    		  res.status(404);
	    		  return ("<html><body><h1>Title not found.</h1></body></html>");
	    	  }
	      });
	      
	      /* GENRE */
	      get("/genre/:genre", (req,res)-> {
	    	  
	    	  MongoCollection<Document> collection = database.getCollection("movies");
		      String filter=req.params("genre").toLowerCase();
		      filter=WordUtils.capitalizeFully(filter);
		      logger.info("Finding genre: " + filter);
		      
		      AggregateIterable<Document> myDoc = collection.aggregate(Arrays.asList(
		    		  Aggregates.match(Filters.eq("genres", filter)),
		    		  Aggregates.project(Projections.include("title", "genres")),
		    		  Aggregates.sort(Sorts.ascending("title")),
		    		  Aggregates.limit(10)
		    		  ));
		      
		      List<String> movies = new ArrayList();
		      try (MongoCursor<Document> iterator = myDoc.iterator()) {
		    	  
		    	  if (!iterator.hasNext()) {
			    	  res.status(404);
		    		  return ("<html><body><h1>Title not found.</h1></body></html>");
		    	  }
		    	  
		    	  while (iterator.hasNext()) {
		    		  movies.add(iterator.next().getString("title"));
		    	  }
		      }
		      
		      res.type("application/json");
		      return new Gson().toJson(movies);
	      });
	      
	      /* ACTOR */
	      get("/fullplot/:title", (req,res)-> {
	    	  
		      MongoCollection<Document> collection = database.getCollection("movies");
		      logger.info("Finding all different cuisines ");
	    	  DistinctIterable<String> col = collection.distinct("plot", String.class);
	    	  JsonArray list = new JsonArray();
	    	  JsonObject myDoc = new JsonObject();
	    	  
	    	  if (col != null) {
	    		  for (String tmp : col) { 
	    			  list.add(tmp);	 
	    		  }
	    	  myDoc.add("plot", list);	  
	    	  } else {
	    		  res.status(404);
	    		  return ("<html><body><h1>No plot found.</h1></body></html>");
	    	  }
	    	  
	          return myDoc;
	      });
	      
	      /*
	      post("/title", (request, response) -> {
	    	  
	    	  response.type("application/json");
	    	  try {
	    	    	
	    		  MongoCollection<Document> collection = database.getCollection("movies");
	    		  collection.insertOne(new Document(Document.parse(request.body())));
	    		  logger.info("Adding Titles ");
	    		   
	            } catch (MongoException me) {
	              System.err.println("Unable to insert due to an error: " + me);
	            }
	    	    
	    	   response.status(202);
	    	   return ("<html><body><h1>Titles Accepted.</h1></body></html>");
	      });*/
	}

}
