package ca.ubc.cs.cpsc210.translink.ui;

import android.content.Context;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.RoutePattern;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A bus route drawer
public class BusRouteDrawer extends MapViewOverlay {
    /**
     * overlay used to display bus route legend text on a layer above the map
     */
    private BusRouteLegendOverlay busRouteLegendOverlay;
    /**
     * overlays used to plot bus routes
     */
    private List<Polyline> busRouteOverlays;

    /**
     * Constructor
     *
     * @param context the application context
     * @param mapView the map view
     */
    public BusRouteDrawer(Context context, MapView mapView) {
        super(context, mapView);
        busRouteLegendOverlay = createBusRouteLegendOverlay();
        busRouteOverlays = new ArrayList<>();
    }

    /**
     * Plot each visible segment of each route pattern of each route going through the selected stop.
     */
    public void plotRoutes(int zoomLevel) {

        updateVisibleArea();
        busRouteLegendOverlay.clear();
        busRouteOverlays.clear();
        if (StopManager.getInstance().getSelected() == null) {
            return;
        }

        for (Route r: StopManager.getInstance().getSelected().getRoutes()) {
            busRouteLegendOverlay.add(r.getNumber());
            List<Polyline> routeLines = polylinesForRoute(r, zoomLevel, busRouteLegendOverlay.getColor(r.getNumber()));
            busRouteOverlays.addAll(routeLines);

        }
    }

    /**
     *
     * @param route routes that are checked to be added to polyline
     * @param zoomLevel how much we can zoom on to the colour
     * @return all the polylines for a particular bus route
     */
    private List<Polyline> polylinesForRoute(Route route, int zoomLevel, int color) {
        List<GeoPoint> gp = new ArrayList<>();
        List<Polyline> routePatterns = new ArrayList<>();
        for (RoutePattern rp: route.getPatterns()) {
            routePatterns.addAll(polylineForRoutePattern(rp, color, zoomLevel));
        }
        return routePatterns;
    }

    private List<Polyline> polylineForRoutePattern(RoutePattern rp, int color, int zoomLevel) {
        ArrayList<LatLon> routePoints = new ArrayList<LatLon>(rp.getPath());
        List<Polyline> result = new ArrayList<>();

        for (int i = 0; i < routePoints.size();) {
            Polyline newLine = lineForSegment(routePoints, i, zoomLevel, color);
            result.add(newLine);
            i += newLine.getPoints().size() - 1;
        }
        return result;
    }


    private int findStartPoint(ArrayList<LatLon> points, int startIndex) {
        int counter = startIndex;
        while (counter < points.size() && !Geometry.rectangleContainsPoint(northWest, southEast, points.get(counter))) {
            counter++;
        }
        return counter == 0 ? 0 : counter - 1;
    }

    private Polyline lineForSegment(ArrayList<LatLon> points, int startIndex, int zoomLevel, int color) {
        int i = findStartPoint(points, startIndex);
        LatLon currentPoint = points.get(i);
        List<GeoPoint> polylinePoints = new ArrayList<>();

        polylinePoints.add(Geometry.gpFromLatLon(currentPoint));
        while (i + 1 < points.size()) {
            ++i;
            currentPoint = points.get(i);

            if (Geometry.rectangleContainsPoint(northWest, southEast, currentPoint)) {
                polylinePoints.add(Geometry.gpFromLatLon(currentPoint));
            } else {
                break;
            }
        }
        polylinePoints.add(Geometry.gpFromLatLon(points.get(i)));
        return helperAddLineForSegment(polylinePoints, zoomLevel, color);
    }

    private Polyline helperAddLineForSegment(List<GeoPoint> polylinePoints, int zoomLevel, int color) {
        Polyline newLine = new Polyline(context);
        newLine.setColor(color);
        newLine.setWidth(getLineWidth(zoomLevel));
        newLine.setPoints(polylinePoints);
        return newLine;
    }

    public List<Polyline> getBusRouteOverlays() {
        return Collections.unmodifiableList(busRouteOverlays);
    }

    public BusRouteLegendOverlay getBusRouteLegendOverlay() {
        return busRouteLegendOverlay;
    }


    /**
     * Create text overlay to display bus route colours
     */
    private BusRouteLegendOverlay createBusRouteLegendOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(context);
        return new BusRouteLegendOverlay(rp, BusesAreUs.dpiFactor());
    }

    /**
     * Get width of line used to plot bus route based on zoom level
     *
     * @param zoomLevel the zoom level of the map
     * @return width of line used to plot bus route
     */
    private float getLineWidth(int zoomLevel) {
        if (zoomLevel > 14) {
            return 7.0f * BusesAreUs.dpiFactor();
        } else if (zoomLevel > 10) {
            return 5.0f * BusesAreUs.dpiFactor();
        } else {
            return 2.0f * BusesAreUs.dpiFactor();
        }
    }
}
