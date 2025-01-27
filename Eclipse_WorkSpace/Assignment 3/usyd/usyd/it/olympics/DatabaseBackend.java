package usyd.it.olympics;


//helpme
/**
* Database back-end class for simple gui.
*
* The DatabaseBackend class defined in this file holds all the methods to
* communicate with the database and pass the results back to the GUI.
*
*
* Make sure you update the dbname variable to your own database name. You
* can run this class on its own for testing without requiring the GUI.
*/
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.rmi.CORBA.UtilDelegate;

/**
* Database interfacing backend for client. This class uses JDBC to connect to
* the database, and provides methods to obtain query data.
*
* Most methods return database information in the form of HashMaps (sets of
* key-value pairs), or ArrayLists of HashMaps for multiple results.
*
* @author Bryn Jeffries {@literal <bryn.jeffries@sydney.edu.au>}
*/
public class DatabaseBackend {

	///////////////////////////////
	/// DB Connection details
	///////////////////////////////
	private final String dbUser;
	private final String dbPass;
	private final String connstring;


	///////////////////////////////
	/// Student Defined Functions
	///////////////////////////////

	/**BLEH BLEH BLEH
	*BLEH
	*ESFAAA
	*/
	private void hashPasswords(){
		Connection conn = null;
		try{
			conn = getConnection();
			String query = "SELECT member_id, pass_word\n"
					+ "FROM Member;\n";
			PreparedStatement stmt = conn.prepareStatement(query);
			ResultSet rset = stmt.executeQuery();
			while(rset.next()){
				String mem_id = rset.getString("member_id");
				if(rset.getString("pass_word") == null)
					continue;
				String new_password = String.valueOf(rset.getString("pass_word").hashCode());
				String update = String.format("UPDATE Member\n"
						+ "SET pass_word = ? \n"
						+ "WHERE member_id = ?\n");
				PreparedStatement updates = conn.prepareStatement(update);
				updates.setString(1, new_password);
				updates.setString(2, mem_id);
				updates.executeUpdate();
			}
		}
		catch(Exception e){
			System.out.println(e);
		}
		finally{
			reallyClose(conn);
		}
	}

	/////  Login and Member  //////

	/**
   * Validate memberID details
   *
   * Implements Core Functionality (a)
   *
   * @return true if username is for a valid memberID and password is correct
   * @throws OlympicsDBException
   * @throws SQLException
   */
	public HashMap<String,Object> checkLogin(String member, char[] password) throws OlympicsDBException  {
		//update database with hashed passwords
		//hashPasswords();
		HashMap<String,Object> details = null;
		Connection conn = null;
		try {
			// FIXME: REPLACE FOLLOWING LINES WITH REAL OPERATION
			// Don't forget you have memberID variables memberUser available to
			// use in a query.
			// Query whether login (memberID, password) is correct...
			conn = getConnection();

			String query = "SELECT * FROM Member WHERE LOWER(member_id) = LOWER(?) AND pass_word = ?;";
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1, member);
			statement.setString(2, new String(password));
			ResultSet rset = statement.executeQuery();
			//FIXME PROTECT ME FROM SQL INJECTIONS
			if(rset.next()){
				details = new HashMap<String,Object>();

				query = "SELECT COUNT(*) FROM Athlete WHERE LOWER(member_id) = LOWER(?);";
				statement = conn.prepareStatement(query);
				statement.setString(1, member);
				ResultSet athlete = statement.executeQuery();

				query = "SELECT COUNT(*) FROM official WHERE LOWER(member_id) = LOWER(?);";
				statement = conn.prepareStatement(query);
				statement.setString(1, member);
				ResultSet official = statement.executeQuery();

				query = "SELECT COUNT(*) FROM staff WHERE LOWER(member_id) = LOWER(?);";
				statement = conn.prepareStatement(query);
				statement.setString(1, member);
				ResultSet staff = statement.executeQuery();
				athlete.next();
				official.next();
				staff.next();
				if(athlete.getInt(1) == 1)
					details.put("member_type", "athlete");
				if(official.getInt(1) == 1)
					details.put("member_type", "official");
				if(staff.getInt(1) == 1)
					details.put("member_type", "staff");
        statement.close();
			}
		}
    catch(Exception e){
			throw new OlympicsDBException("Error checking login details", e);
		}
		finally{
			reallyClose(conn);
		}
		return details;
	}

	/**
   * Obtain details for the current memberID
   * @param memberID
   * @param member_type
   *
   *
   * @return text to be displayed in the home screen
   * @throws OlympicsDBException
   * @throws SQLException
   */
	public HashMap<String, Object> getMemberDetails(String memberID) throws OlympicsDBException {
		HashMap<String, Object> details = new HashMap<String, Object>();
		Connection conn = null;
		try{
			conn = getConnection();
			details = new HashMap<String,Object>();
			String query = "SELECT COUNT(*) FROM Athlete WHERE LOWER(member_id) = LOWER(?)";
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1, memberID);
			ResultSet athlete = statement.executeQuery();
			athlete.next();
			query = "SELECT COUNT(*) FROM official WHERE LOWER(member_id) = LOWER(?)";
			statement = conn.prepareStatement(query);
			statement.setString(1, memberID);
			ResultSet official= statement.executeQuery();
			official.next();
			query = "SELECT COUNT(*) FROM staff WHERE LOWER(member_id) = LOWER(?)";
			statement = conn.prepareStatement(query);
			statement.setString(1, memberID);
			ResultSet staff= statement.executeQuery();
			staff.next();
			if(athlete.getInt(1) == 1)
				details.put("member_type", "athlete");
			if(official.getInt(1) == 1)
				details.put("member_type", "official");
			if(staff.getInt(1) == 1)
				details.put("member_type", "staff");
			query = "SELECT *"
				+ "FROM Member NATURAL JOIN\n"
				+ "Country JOIN\n"
				+ "Place ON (accommodation = place_id)\n"
				+ "WHERE member_id = ?;";
			statement = conn.prepareStatement(query);
			statement.setString(1, memberID);
			ResultSet rset = statement.executeQuery();
			rset.next();
			details.put("member_id", memberID);
			details.put("title", rset.getString("title"));
			details.put("first_name", rset.getString("given_names"));
			details.put("family_name", rset.getString("family_name"));
			details.put("country_name", rset.getString("country_name"));
			details.put("residence", rset.getString("place_name"));

			//TODO

			query = "SELECT COUNT(*)\n"
				+ "FROM booking\n"
				+ "WHERE booked_for  = ?;";
			statement = conn.prepareStatement(query);
			statement.setString(1, memberID);
			rset = statement.executeQuery();
			rset.next();
			details.put("num_bookings", rset.getInt("count"));

			// Some attributes fetched may depend upon member_type
			// This is for an athlete
			//TODO
			if(details.get("member_type").equals("athlete")){
				int gold_count = 0;
				int silver_count = 0;
				int bronze_count = 0;
				query = "SELECT COUNT(*) FROM "
						+ "(SELECT medal FROM Team NATURAL JOIN TeamMember "
						+ "WHERE athlete_id = ? "
						+ "AND medal = 'G' "
						+ "UNION ALL "
						+ "SELECT medal "
						+ "FROM Participates "
						+ "WHERE athlete_id = ? "
						+ "AND medal = 'G') as count_gold;";
				statement = conn.prepareStatement(query);
				statement.setString(1, memberID);
				statement.setString(2, memberID);
				rset = statement.executeQuery();
				rset.next();
				gold_count+=rset.getInt(1);
				details.put("num_gold", gold_count);

				query = "SELECT COUNT(*) FROM "
						+ "(SELECT medal FROM Team NATURAL JOIN TeamMember "
						+ "WHERE athlete_id = ? "
						+ "AND medal = 'S' "
						+ "UNION ALL "
						+ "SELECT medal "
						+ "FROM Participates "
						+ "WHERE athlete_id = ? "
						+ "AND medal = 'S') as count_silver;";
				statement = conn.prepareStatement(query);
				statement.setString(1, memberID);
				statement.setString(2, memberID);
				rset = statement.executeQuery();
				rset.next();
				silver_count+=rset.getInt(1);
				details.put("num_silver", silver_count);

				query = "SELECT COUNT(*) FROM "
						+ "(SELECT medal FROM Team NATURAL JOIN TeamMember "
						+ "WHERE athlete_id = ? "
						+ "AND medal = 'B' "
						+ "UNION ALL "
						+ "SELECT medal "
						+ "FROM Participates "
						+ "WHERE athlete_id = ? "
						+ "AND medal = 'B') as count_bronze;";
				statement = conn.prepareStatement(query);
				statement.setString(1, memberID);
				statement.setString(2, memberID);
				rset = statement.executeQuery();
				rset.next();
				bronze_count+=rset.getInt(1);
				details.put("num_bronze", bronze_count);

			}
			statement.close();
		}
		catch(Exception e){
			System.err.println(e);
			throw new OlympicsDBException("Error checking member details", e);
		}
		finally{
			reallyClose(conn);
		}

		return details;
	}


	//////////  Events  //////////

	/**
   * Get all of the events listed in the olympics for a given sport
   *
   * @param sportname the ID of the sport we are filtering by
   * @return List of the events for that sport
   * @throws OlympicsDBException
   * @throws SQLException
   */
	ArrayList<HashMap<String, Object>> getEventsOfSport(Integer sportname) throws OlympicsDBException {
		// FIXME: Replace the following with REAL OPERATIONS!

		ArrayList<HashMap<String, Object>> events = new ArrayList<>();
		Connection conn = null;
		try{
			conn = getConnection();

			String query = "SELECT event_id, sport_id, event_name, event_gender, place_name, event_start\n"
				+ "FROM Sport NATURAL JOIN\n"
				+ "Event JOIN\n"
				+ "Place ON (sport_venue = place_id)\n"
				+ "WHERE sport_id = ?;";


			PreparedStatement statement = conn.prepareStatement(query);
			statement.setInt(1, sportname);
			//System.out.println(query);
			ResultSet rset = statement.executeQuery();
			Timestamp event_start;
			while(rset.next()){
				HashMap<String,Object> event = new HashMap<String,Object>();
				event.put("event_id", rset.getInt("event_id"));
				event.put("sport_id", rset.getInt("sport_id"));
				event.put("event_name", rset.getString("event_name"));
				event.put("event_gender", rset.getString("event_gender"));
				event.put("sport_venue", rset.getString("place_name"));
				//String real_event_time = "";
				//String day = "";
				//day, month, day num, time without millisecond, timezone, year
				//real_event_time = real_event_time + " AEST ";
				event_start = rset.getTimestamp(6);
				//Date event_date = new Date(event_start.getTime());
				event.put("event_start", rset.getTimestamp("event_start"));
				events.add(event);
			}

			statement.close();
			rset.close();
		}
		catch(SQLException e){
      System.err.println(e);
			throw new OlympicsDBException("Error checking member details", e);
		}
		finally{

			reallyClose(conn);
		}
		return events;
	}
	
	//overloaded sexed up
	ArrayList<HashMap<String, Object>> getEventsOfSport(Integer sportname, String sex) throws OlympicsDBException {
		// FIXME: Replace the following with REAL OPERATIONS!

		ArrayList<HashMap<String, Object>> events = new ArrayList<>();
		Connection conn = null;
		try{
			conn = getConnection();

			String query = "SELECT event_id, sport_id, event_name, event_gender, place_name, event_start\n"
				+ "FROM Sport NATURAL JOIN\n"
				+ "Event JOIN\n"
				+ "Place ON (sport_venue = place_id)\n"
				+ "WHERE sport_id = ?\n"
				+ "AND event_gender = ?;";


			PreparedStatement statement = conn.prepareStatement(query);
			statement.setInt(1, sportname);
			statement.setString(2, sex);
			//System.out.println(query);
			ResultSet rset = statement.executeQuery();
			Timestamp event_start;
			while(rset.next()){
				HashMap<String,Object> event = new HashMap<String,Object>();
				event.put("event_id", rset.getInt("event_id"));
				event.put("sport_id", rset.getInt("sport_id"));
				event.put("event_name", rset.getString("event_name"));
				event.put("event_gender", rset.getString("event_gender"));
				event.put("sport_venue", rset.getString("place_name"));
				//String real_event_time = "";
				//String day = "";
				//day, month, day num, time without millisecond, timezone, year
				//real_event_time = real_event_time + " AEST ";
				event_start = rset.getTimestamp(6);
				//Date event_date = new Date(event_start.getTime());
				event.put("event_start", rset.getTimestamp("event_start"));
				events.add(event);
			}

			statement.close();
			rset.close();
		}
		catch(SQLException e){
      System.err.println(e);
			throw new OlympicsDBException("Error checking member details", e);
		}
		finally{

			reallyClose(conn);
		}
		return events;
	}

	/**
   * Retrieve the results for a single event
   * @param eventId the key of the event
   * @return a hashmap for each result in the event.
   * @throws OlympicsDBException
   * @throws SQLException
   */
	ArrayList<HashMap<String, Object>> getResultsOfEvent(Integer eventId) throws OlympicsDBException {
		// FIXME: Replace the following with REAL OPERATIONS!

		ArrayList<HashMap<String, Object>> results = new ArrayList<>();
		Connection conn = null;
		try{
			conn = getConnection();

			String query = "SELECT COUNT(*) FROM TeamEvent WHERE event_id = ?;";
			System.out.println(query);
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setInt(1, eventId);
			ResultSet rset = statement.executeQuery();
			rset.next();
			int isTeamEvent = rset.getInt(1);

			if (isTeamEvent == 0) {

				query = "SELECT family_name, given_names, country_name, medal\n"
					+ "FROM Event NATURAL JOIN\n"
					+ "Participates JOIN\n"
					+ "Member ON (athlete_id = member_id) NATURAL JOIN\n"
					+ "Country\n"
					+ "WHERE event_id = ?\n"
					+ "ORDER BY family_name;";
				System.out.println(query);
				statement = conn.prepareStatement(query);
				statement.setInt(1, eventId);
				rset = statement.executeQuery();
				while(rset.next()){
					HashMap<String, Object> result = new HashMap<>();
					result.put("participant", String.format("%s, %s",rset.getString("family_name"), rset.getString("given_names")));
					result.put("country_name", rset.getString("country_name"));
					if(rset.getString("medal") == null)
						result.put("medal", null);
					else
						switch(rset.getString("medal")){
							case "G":
								result.put("medal", "Gold");
								break;
							case "S":
								result.put("medal", "Silver");
								break;
							case "B":
								result.put("medal", "Bronze");
								break;
						}
					results.add(result);
				}
			}
			else {
				query = "SELECT * "
						+ "FROM Team NATURAL JOIN TeamEvent NATURAL JOIN Country "
						+ "where event_id = ?"
						+ "ORDER BY team_name;";
				System.out.println(query);
				statement = conn.prepareStatement(query);
				statement.setInt(1, eventId);
				rset = statement.executeQuery();
				while (rset.next()) {
					HashMap<String, Object> result = new HashMap<>();
					result.put("participant", rset.getString("team_name"));
					result.put("country_name", rset.getString("country_name"));
					if(rset.getString("medal") == null)
						result.put("medal", null);
					else
						switch(rset.getString("medal")){
							case "G":
								result.put("medal", "Gold");
								break;
							case "S":
								result.put("medal", "Silver");
								break;
							case "B":
								result.put("medal", "Bronze");
								break;
						}
					results.add(result);

				}

			}
			statement.close();
		}
		catch(SQLException e){
      System.err.println(e);
			throw new OlympicsDBException("Error checking member details", e);
		}
		finally{
			reallyClose(conn);
		}

		return results;
	}


	///////   Journeys    ////////

	/**
	 * Array list of journeys from one place to another on a given date
	 * @param journeyDate the date of the journey
	 * @param fromPlace the origin, starting place.
	 * @param toPlace the destination, place to go to.
	 * @return a list of all journeys from the origin to destination
	 * @throws SQLException
	 */
	ArrayList<HashMap<String, Object>> findJourneys(String fromPlace, String toPlace, Date journeyDate) throws OlympicsDBException {
	  // FIXME: Replace the following with REAL OPERATIONS!
	  ArrayList<HashMap<String, Object>> journeys = new ArrayList<>();

	  Connection conn = null;
	  try{
	    conn = getConnection();
	    //FIXME THING REPLACE '=' WITH LIKE
	    String query = "SELECT journey_id, vehicle_code, P1.place_name as fromp, P2.place_name as top, "
	                   + "depart_time, arrive_time, capacity, nbooked\n"
	                   + "FROM Place P2 JOIN (Journey NATURAL JOIN Vehicle) ON (to_place = P2.place_id)"
	                   + "JOIN Place P1 ON (from_place = P1.place_id)\n"
	                   + "WHERE P1.place_name LIKE ?\n"
	                   + "AND P2.place_name LIKE ?\n"
	                   + "AND CAST(depart_time AS DATE) = ?;";

	    PreparedStatement statement = conn.prepareStatement(query);
	    System.out.println(query);
	    statement.setString(1, fromPlace);
	    statement.setString(2, toPlace);
	    statement.setDate(3, new java.sql.Date(journeyDate.getTime()));

	    ResultSet rset = statement.executeQuery();
	    while(rset.next()){
	      HashMap<String,Object> journey = new HashMap<String,Object>();
	      journey.put("journey_id", rset.getInt("journey_id"));
	      journey.put("vehicle_code", rset.getString("vehicle_code"));
	      journey.put("origin_name", rset.getString("fromp"));
	      journey.put("dest_name", rset.getString("top"));
	      Date when_departs = new Date(rset.getTimestamp("depart_time").getTime());
	      journey.put("when_departs", when_departs);
	      Date when_arrives = new Date(rset.getTimestamp("arrive_time").getTime());
	      journey.put("when_arrives", when_arrives);
	      journey.put("available_seats", Integer.valueOf(rset.getInt("capacity")-rset.getInt("nbooked")));
	      journeys.add(journey);

	    }
	    statement.close();
	  }
	  catch(SQLException e){
	    System.err.println(e);
	    throw new OlympicsDBException("Error finding journey", e);
	  }
	  finally{
	    reallyClose(conn);
	  }
	  return journeys;
	}
	
	//OVERLOADED METHOD WITH LIMIT IMPLEMENTATION
	ArrayList<HashMap<String, Object>> findJourneys(String fromPlace, String toPlace, Date journeyDate, int limit) throws OlympicsDBException {
		  // FIXME: Replace the following with REAL OPERATIONS!
		  ArrayList<HashMap<String, Object>> journeys = new ArrayList<>();

		  Connection conn = null;
		  try{
		    conn = getConnection();
		    //FIXME THING REPLACE '=' WITH LIKE
		    String query = "SELECT journey_id, vehicle_code, P1.place_name as fromp, P2.place_name as top, "
		                   + "depart_time, arrive_time, capacity, nbooked\n"
		                   + "FROM Place P2 JOIN (Journey NATURAL JOIN Vehicle) ON (to_place = P2.place_id)"
		                   + "JOIN Place P1 ON (from_place = P1.place_id)\n"
		                   + "WHERE P1.place_name LIKE ?\n"
		                   + "AND P2.place_name LIKE ?\n"
		                   + "AND CAST(depart_time AS DATE) = ?\n"
		                   + "LIMIT ?;";

		    PreparedStatement statement = conn.prepareStatement(query);
		    System.out.println(query);
		    statement.setString(1, fromPlace);
		    statement.setString(2, toPlace);
		    statement.setInt(4, limit);
		    statement.setDate(3, new java.sql.Date(journeyDate.getTime()));

		    ResultSet rset = statement.executeQuery();
		    while(rset.next()){
		      HashMap<String,Object> journey = new HashMap<String,Object>();
		      journey.put("journey_id", rset.getInt("journey_id"));
		      journey.put("vehicle_code", rset.getString("vehicle_code"));
		      journey.put("origin_name", rset.getString("fromp"));
		      journey.put("dest_name", rset.getString("top"));
		      Date when_departs = new Date(rset.getTimestamp("depart_time").getTime());
		      journey.put("when_departs", when_departs);
		      Date when_arrives = new Date(rset.getTimestamp("arrive_time").getTime());
		      journey.put("when_arrives", when_arrives);
		      journey.put("available_seats", Integer.valueOf(rset.getInt("capacity")-rset.getInt("nbooked")));
		      journeys.add(journey);

		    }
		    statement.close();
		  }
		  catch(SQLException e){
		    System.err.println(e);
		    throw new OlympicsDBException("Error finding journey", e);
		  }
		  finally{
		    reallyClose(conn);
		  }
		  return journeys;
		}
	
	
	//TODO
	ArrayList<HashMap<String,Object>> getMemberBookings(String memberID) throws OlympicsDBException {
	  ArrayList<HashMap<String,Object>> bookings = new ArrayList<HashMap<String,Object>>();
	  Connection conn = null;
	  try{
	    conn = getConnection();

	    String query = "SELECT journey_id, vehicle_code, P1.place_name as fromp, P2.place_name as top, depart_time, arrive_time\n"
	      + "FROM Booking NATURAL JOIN ((Journey JOIN Place P2 ON (to_place = P2.place_id)) JOIN Place P1 ON (from_place = P1.place_id))\n"
	      + "where booked_for = ?;";
	    PreparedStatement statement = conn.prepareStatement(query);
	    statement.setString(1, memberID);
	    ResultSet rset = statement.executeQuery();
	    while(rset.next()){
	      HashMap<String, Object> booking = new HashMap<>();
	      booking.put("journey_id", rset.getInt("journey_id"));
	      booking.put("vehicle_code", rset.getString("vehicle_code"));
	      booking.put("origin_name", rset.getString("fromp"));
	      booking.put("dest_name", rset.getString("top"));
	      booking.put("when_departs", rset.getTimestamp("depart_time"));
	      booking.put("when_arrives", rset.getTimestamp("arrive_time"));
	      bookings.add(booking);
	    }

	    statement.close();

	  }
	  catch(SQLException e){
	    System.err.println(e);
	    throw new OlympicsDBException("Error getting booking details", e);
	  }
	  finally{
	    reallyClose(conn);
	  }
	  return bookings;
	}

	/**
	 * Get details for a specific journey
	 *
	 * @return Various details of journey - see JourneyDetails.java
	 * @throws OlympicsDBException
	 */
	public HashMap<String,Object> getJourneyDetails(int journey_id) throws OlympicsDBException {
	  //TODO

	  // FIXME: REPLACE FOLLOWING LINES WITH REAL OPERATION

	  HashMap<String,Object> details = new HashMap<String,Object>();


	  Connection conn = null;
	  try{
	    conn = getConnection();


	    String query = "SELECT journey_id, vehicle_code, P1.place_name as fromp, P2.place_name as top, "
	                   + "depart_time, arrive_time, capacity, nbooked\n"
	                   + "FROM Vehicle NATURAL JOIN ((Journey JOIN Place P2 ON (to_place = P2.place_id)) "
	                   + "JOIN Place P1 ON (from_place = P1.place_id))\n"
	                   + "WHERE journey_id = ?";
	    System.out.println(query);
	    PreparedStatement statement = conn.prepareStatement(query);
	    statement.setInt(1, journey_id);
	    ResultSet rset = statement.executeQuery();

	    while(rset.next()) {
	      details.put("journey_id", rset.getInt("journey_id"));
	      details.put("vehicle_code", rset.getString("vehicle_code"));
	      details.put("origin_name", rset.getString("fromp"));
	      details.put("dest_name", rset.getString("top"));
	      details.put("when_departs", rset.getTimestamp("depart_time"));
	      details.put("when_arrives", rset.getTimestamp("arrive_time"));
	      details.put("capacity", rset.getInt("capacity"));
	      details.put("nbooked", rset.getInt("nbooked"));
	    }

	    statement.close();
	  }
	  catch(SQLException e){

	  }
	  finally{
	    reallyClose(conn);
	  }


	  return details;
	}

	public HashMap<String,Object> makeBooking(String byStaff, String forMember, String vehicle, Date departs) throws OlympicsDBException {
		HashMap<String,Object> booking = new HashMap<String,Object>();
		//TODO
		// FIXME: DUMMY FUNCTION NEEDS TO BE PROPERLY IMPLEMENTED
		Connection conn = null;
		try{
			
			conn = getConnection();
			conn.setAutoCommit(false);
			String query = String.format("SELECT COUNT(*) "
					+ "FROM Journey NATURAL JOIN Vehicle\n"
					+ "WHERE depart_time = '%s'\n"
					+ "AND vehicle_code = '%s'\n"
					+ "AND nbooked < capacity;", new Timestamp(departs.getTime()), vehicle);
			PreparedStatement statement = conn.prepareStatement(query);
			ResultSet rset = statement.executeQuery();
			rset.next();
			int count = rset.getInt(1);
			
			//TODO MAKE THIS A TRANSACTION
			
			if (count < 1){
				conn.rollback();
				//null will not be returned since finally block won't allow it
				booking = new HashMap<>();
				return null;
			}
			query = String.format("SELECT journey_id, vehicle_code, P1.place_name as fromp, P2.place_name as top, "
								  + "depart_time, arrive_time, capacity, nbooked\n"
								  + "FROM Vehicle NATURAL JOIN ((Journey JOIN Place P2 ON (to_place = P2.place_id)) "
								  + "JOIN Place P1 ON (from_place = P1.place_id))\n"
								  + "WHERE vehicle_code = '%s' and depart_time = '%s'", vehicle, new Timestamp(departs.getTime()));
			statement = conn.prepareStatement(query);
			rset = statement.executeQuery();
			System.out.println(query);
			rset.next();
			int journey = rset.getInt("journey_id");
			booking.put("vehicle", rset.getString("vehicle_code"));
			//make gui appear correctly
			booking.put("vehicle_code", rset.getString("vehicle_code"));
			booking.put("when_departs", rset.getTimestamp("depart_time"));
			booking.put("when_arrives", rset.getTimestamp("arrive_time"));
			booking.put("start_day", rset.getDate("depart_time"));
			booking.put("dest_name", rset.getString("top"));
			booking.put("origin_name", rset.getString("fromp"));
			java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
			booking.put("when_booked", ts);
			
			query = String.format("SELECT family_name, given_names\n"
					+ "FROM Member\n"
					+ "WHERE member_id = '%s'\n", byStaff);
			statement = conn.prepareStatement(query);
			rset = statement.executeQuery();
			System.out.println(query);
			rset.next();
			booking.put("bookedby_name", String.format("%s, %s", rset.getString("family_name"), rset.getString("given_names"))) ;
		
			query = String.format("SELECT family_name, given_names\n"
					+ "FROM Member\n"
					+ "WHERE member_id = '%s'\n", forMember);
			statement = conn.prepareStatement(query);
			rset = statement.executeQuery();
			System.out.println(query);
			rset.next();
			booking.put("bookedfor_name", String.format("%s, %s", rset.getString("family_name"), rset.getString("given_names") ));
			String insert = "INSERT INTO BOOKING VALUES(?, ?, ?, ?)";
			PreparedStatement insertstmt = conn.prepareStatement(insert);
			insertstmt.setInt(4, journey);
			insertstmt.setTimestamp(3, ts);
			insertstmt.setString(2, byStaff);
			insertstmt.setString(1, forMember);
			insertstmt.executeUpdate();
			String update = String.format("UPDATE Journey\n"
					+ "SET nbooked = nbooked+1\n"
					+ "WHERE journey_id = %d;", journey);
			insertstmt = conn.prepareStatement(update);
			insertstmt.executeUpdate();
			conn.commit();
			statement.close();



		}
    catch(SQLException e){
      System.err.println(e.getMessage());
			throw new OlympicsDBException("Error making booking", e);
		}
    finally{
			reallyClose(conn);
		}

		return booking;
	}

	public HashMap<String,Object> getBookingDetails(String memberID, Integer journeyId) throws OlympicsDBException {
	  HashMap<String,Object> booking = new HashMap<String, Object>();
	  Connection conn = null;
	  try{
	    conn = getConnection();
	    String query = "SELECT journey_id, vehicle_code, depart_time, arrive_time, when_booked,\n"
	                   + "P2.place_name as fromp, P1.place_name as top, M.family_name as m_last, M.given_names as m_first, S.family_name as s_last, S.given_names as s_first\n"
	                   + "FROM ((Booking JOIN Member M on (booked_for = M.member_id))\n"
	                   + "JOIN Member S on (booked_by = S.member_id))\n"
	                   + "NATURAL JOIN ((Journey JOIN Place P1 ON (P1.place_id = to_place))\n"
	                   + "JOIN Place P2 ON (P2.place_id = from_place))\n"
	                   + "WHERE journey_id = ? AND booked_for = ?\n"
	                   + "ORDER BY depart_time DESC;";
	    System.out.println(query);
	    PreparedStatement statement = conn.prepareStatement(query);
	    statement.setInt(1, journeyId);
	    statement.setString(2, memberID);

	    ResultSet rset = statement.executeQuery();
	    while(rset.next()){
	      booking.put("journey_id", rset.getInt("journey_id"));
	      booking.put("vehicle", rset.getString("vehicle_code"));
	      //making it appear on gui correctly
	      booking.put("vehicle_code", rset.getString("vehicle_code"));
	      booking.put("when_departs", rset.getTimestamp("depart_time"));
	      booking.put("dest_name", rset.getString("top"));
	      booking.put("origin_name", rset.getString("fromp"));
	      booking.put("bookedby_name", String.format("%s, %s", rset.getString("s_last"), rset.getString("s_first")));
	      booking.put("bookedfor_name", String.format("%s, %s", rset.getString("m_last"), rset.getString("m_first")));
	      booking.put("when_booked", rset.getTimestamp("when_booked"));
	      booking.put("when_arrives", rset.getTimestamp("arrive_time"));

	    }

	    statement.close();
	  }
	  catch(SQLException e){
	    System.err.println(e);
	    throw new OlympicsDBException("Error checking booking details", e);
	  }
	  finally{
	    reallyClose(conn);
	  }
	  return booking;
	}

	public ArrayList<HashMap<String, Object>> getSports() throws OlympicsDBException {
	  ArrayList<HashMap<String,Object>> sports = new ArrayList<HashMap<String,Object>>();
	  Connection conn = null;
	  try{
	    conn = getConnection();
	    String query = "SELECT sport_id, sport_name, discipline FROM Sport;";
	    PreparedStatement statement = conn.prepareStatement(query);

	    ResultSet rset = statement.executeQuery();
	    while(rset.next()){
	      HashMap<String, Object> sport = new HashMap<>();
	      sport.put("sport_id", Integer.valueOf(rset.getInt("sport_id")));
	      sport.put("sport_name", rset.getString("sport_name"));
	      sport.put("discipline", rset.getString("discipline"));
	      sports.add(sport);
	    }

	    statement.close();
	    rset.close();
	  }
	  catch(SQLException e){
	    System.err.println(e);
	    throw new OlympicsDBException("Error fetching sports from database", e);
	  }
	  finally{
	    reallyClose(conn);
	  }
	  return sports;
	}


	/////////////////////////////////////////
	/// Functions below don't need
	/// to be touched.
	///
	/// They are for connecting and handling errors!!
	/////////////////////////////////////////

	/**
	 * Default constructor that simply loads the JDBC driver and sets to the
	 * connection details.
	 *
	 * @throws ClassNotFoundException if the specified JDBC driver can't be
	 * found.
	 * @throws OlympicsDBException anything else
	 */
	DatabaseBackend(InputStream config) throws ClassNotFoundException, OlympicsDBException {
	  Properties props = new Properties();
	  try{
	    props.load(config);
	  }
	  catch(IOException e){
	    throw new OlympicsDBException("Couldn't read config data",e);
	  }

	  dbUser = props.getProperty("username");
	  dbPass = props.getProperty("userpass");
	  String port = props.getProperty("port");
	  String dbname = props.getProperty("dbname");
	  String server = props.getProperty("address");;

	  // Load JDBC driver and setup connection details
	  String vendor = props.getProperty("dbvendor");
	  if(vendor==null)
	    throw new OlympicsDBException("No vendor config data");
	  else if("postgresql".equals(vendor)){
	    Class.forName("org.postgresql.Driver");
	    connstring = "jdbc:postgresql://" + server + ":" + port + "/" + dbname;
	  }
	  else if("oracle".equals(vendor)){
	    Class.forName("oracle.jdbc.driver.OracleDriver");
	    connstring = "jdbc:oracle:thin:@" + server + ":" + port + ":" + dbname;
	  }
	  else
	    throw new OlympicsDBException("Unknown database vendor: " + vendor);

	  // test the connection
	  Connection conn = null;
	  try{
	    conn = getConnection();
	  }
	  catch(SQLException e){
	    throw new OlympicsDBException("Couldn't open connection", e);
	  }
	  finally{
	    reallyClose(conn);
	  }
	}

	/**
	* Utility method to ensure a connection is closed without
	* generating any exceptions
	* @param conn Database connection
	*/
	private void reallyClose(Connection conn) {
	  if(conn!=null)
	    try{
	      conn.close();
	    }
	  catch(SQLException ignored){}
	}

	/**
	 * Construct object with open connection using configured login details
	 * @return database connection
	 * @throws SQLException if a DB connection cannot be established
	 */
	private Connection getConnection() throws SQLException {
	  return DriverManager.getConnection(connstring, dbUser, dbPass);
	}

	}
