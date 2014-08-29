package models;

import play.db.ebean.Model;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Route extends Model {

	@Id
	public String id;

	public String source;
	public String destination;
	public Integer timeout;
	public Integer cache;
	public Double randomSeed;
	
	public static Finder<Long, Route> find = new Finder<Long,Route>(
		Long.class, Route.class
	); 

}