package controllers;

import static play.libs.Json.toJson;

import java.util.List;

import models.Route;
import play.Logger;
import play.Routes;
import play.cache.Cache;
import play.data.Form;
import play.db.ebean.Model;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.Administration.*;

public class Administration extends Controller {
	
    public static Result route() {
        return ok(route.render());
    }

    public static Result editRoute(String id) {
        return ok(editroute.render(Route.find.where().eq("id", id).findUnique()));
    }

    public static Result saveRoute() {
    	Route route = Form.form(Route.class).bindFromRequest().get();
		Cache.remove("source:" + route.randomSeed.toString());
		Logger.info("deleted randomSeed " + route.randomSeed + " for route " + route.source);
    	route.randomSeed = Math.random();    	
		Logger.info("created randomSeed " + route.randomSeed + " for route " + route.source);
    	route.update();
		Cache.remove("route:" + route.source);
    	return redirect (routes.Administration.editRoute(route.id));
    }

    public static Result addRoute() {
    	Route route = Form.form(Route.class).bindFromRequest().get();
    	route.randomSeed = Math.random();
    	route.save();
    	return redirect(routes.Administration.routesList());
    }

    public static Result routesList() {
    	List<Route> routes = (List<Route>)new Model.Finder(String.class, Route.class).all();
    	return ok(routesList.render(routes));
    }
}
