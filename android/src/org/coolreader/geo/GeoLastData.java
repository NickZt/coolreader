package org.coolreader.geo;

import android.location.Location;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.StrUtils;

import java.util.ArrayList;
import java.util.List;

public class GeoLastData {

    public static Double MIN_DIST_NEAR = 100D; // meters
    public static Double MIN_DIST_STATION_SIGNAL1 = 200D; // meters
    public static Double MIN_DIST_STATION_SIGNAL2 = 700D; // meters
    public static Double MIN_DIST_STOP_SIGNAL1 = 100D; // meters
    public static Double MIN_DIST_STOP_SIGNAL2 = 400D; // meters
    // geo work - system objects
    public CoolReader coolReader = null;
    public ProviderLocationTracker gps = null;
    public ProviderLocationTracker netw = null;
    public LocationTracker.LocationUpdateListener geoListener = null;
    public LocationTracker.LocationUpdateListener netwListener = null;
    public static List<MetroLocation> metroLocations = new ArrayList<MetroLocation>();
    public static List<TransportStop> transportStops = new ArrayList<TransportStop>();
    // logic
    public Double lastLon = 0D;
    public Double lastLat = 0D;
    public MetroStation lastStationBefore = null;
    public MetroStation lastStation = null;
    public MetroStation tempStation = null; //!!!! temp
    public TransportStop tempStop = null; //!!!! temp
    public Double lastStationDist = -1D;
    public boolean near2signalled = false;
    public boolean near1signalled = false;
    public TransportStop lastStopBefore = null;
    public TransportStop lastStop = null;
    public Double lastStopDist = -1D;

    public GeoLastData(CoolReader cr) {
        coolReader = cr;
    }

    public void setLastStation(Double lon, Double lat, MetroStation lastSt) {
        lastStationBefore = lastStation;
        lastStation = lastSt;
        lastStationDist = -1D;
        if (lastSt!=null)
            lastStationDist = geoDistance(lat, lastSt.lat, lon, lastSt.lon,0D, 0D);
    }

    public void setLastStop(Double lon, Double lat, TransportStop lastSt) {
        lastStopBefore = lastStop;
        lastStop = lastSt;
        lastStopDist = -1D;
        if (lastSt!=null)
            lastStopDist = geoDistance(lat, lastSt.lat, lon, lastSt.lon,0D, 0D);
    }

    public void updateDistance(Double lon, Double lat) {
        if (lastStation!=null)
            lastStationDist = geoDistance(lat, lastStation.lat, lon, lastStation.lon,0D, 0D);
        if (lastStop!=null)
            lastStopDist = geoDistance(lat, lastStop.lat, lon, lastStop.lon,0D, 0D);
    }

    public void checkSingnalled(boolean bSameStation, boolean bSameStop) {
        if (near1signalled || near2signalled) return; // if everything already done
        boolean bStationInDist1 = (lastStationDist>0) && (lastStationDist < MIN_DIST_STATION_SIGNAL1);
        boolean bStationInDist2 = (lastStationDist>0) && (lastStationDist < MIN_DIST_STATION_SIGNAL2);
        boolean bStopInDist1 = (lastStopDist>0) && (lastStopDist < MIN_DIST_STOP_SIGNAL1);
        boolean bStopInDist2 = (lastStopDist>0) && (lastStopDist < MIN_DIST_STOP_SIGNAL2);
        boolean bNeedSignal1 = bStationInDist1 || bStopInDist1;
        boolean bNeedSignal2 = bStationInDist2 || bStopInDist2;
        if (bNeedSignal1) {
            if (!near1signalled) doSignal(bSameStation, bSameStop);
            near1signalled = true;
            near2signalled = true;
        }
        if (bNeedSignal2 && (!bNeedSignal1)) {
            if (!near2signalled) doSignal(bSameStation, bSameStop);
            near2signalled = true;
        }
    }

    public void doSignal(final boolean bSameStation, final boolean bSameStop) {
        String s = "";
        if (lastStation != null) s = lastStation.name + " (" + lastStationDist.intValue() + "m)";
        if (lastStop != null) s = s + "; " + lastStop.name + " (" + lastStopDist.intValue() + "m), "+lastStop.routeNumbers;
        if (s.startsWith(";")) s=s.substring(2);
        if (!StrUtils.isEmptyStr(s)) {
            final String sF = s;
            BackgroundThread.instance().postBackground(new Runnable() {
                @Override
                public void run() {
                    BackgroundThread.instance().postGUI(new Runnable() {
                        @Override
                        public void run() {
                            coolReader.showGeoToast(sF, lastStation, lastStop, lastStationDist, lastStopDist, lastStationBefore,
                                    bSameStation, bSameStop);
                        }
                    }, 500);
                }
            });
        }
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double geoDistance(double lat1, double lat2, double lon1,
                                     double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public MetroStation getClosestStation(Double lon, Double lat, MetroStation exceptS) {
        double dist = -1;
        double closestDist = -1;
        MetroStation closestStation = null;
        for (MetroLocation ml: metroLocations) {
            for (MetroLine mll: ml.metroLines) {
                for (MetroStation ms: mll.metroStations) {
                    if (exceptS!=null)
                        if (ms.equals(exceptS)) continue;
                    dist = geoDistance(lat, ms.lat, lon, ms.lon,0D, 0D);
                    if ((dist<closestDist)||(closestDist<0)) {
                        closestDist = dist;
                        closestStation = ms;
                    }
                }
            }
        }
        return closestStation;
    }

    public TransportStop getClosestStop(Double lon, Double lat, TransportStop exceptS) {
        double dist = -1;
        double closestDist = -1;
        TransportStop closestStop = null;
        for (TransportStop ts: transportStops) {
            if (exceptS!=null)
                if (ts.equals(exceptS)) continue;
            dist = geoDistance(lat, ts.lat, lon, ts.lon,0D, 0D);
            if ((dist<closestDist)||(closestDist<0)) {
                closestDist = dist;
                closestStop = ts;
            }
        }
        return closestStop;
    }

    public boolean isNearClosestStation(Double lon, Double lat, MetroStation closestStation, MetroStation closeStation) {
        if ((closestStation != null) && (closeStation != null)) {
            Double dist = geoDistance(lat, closestStation.lat, lon, closestStation.lon,0D, 0D);
            Double stationsDist = geoDistance(closestStation.lat, closeStation.lat,
                    closestStation.lon, closeStation.lon, 0D, 0D);
            if ((dist<stationsDist) || (dist<MIN_DIST_NEAR)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNearClosestStop(Double lon, Double lat, TransportStop closestStop, TransportStop closeStop) {
        if ((closestStop != null) && (closeStop != null)) {
            Double dist = geoDistance(lat, closestStop.lat, lon, closestStop.lon,0D, 0D);
            Double stationsDist = geoDistance(closestStop.lat, closeStop.lat,
                    closestStop.lon, closeStop.lon, 0D, 0D);
            if ((dist<stationsDist) || (dist<MIN_DIST_NEAR)) {
                return true;
            }
        }
        return false;
    }

    public void geoUpdateCoords(Location oldLoc, long oldTime, Location newLoc,
                                long newTime) {
        final Double longitude = newLoc.getLongitude();
        final Double latitude = newLoc.getLatitude();
        final Double llastLon = lastLon;
        final Double llastLat = lastLat;
        //Double height = newLoc.getAltitude();
        //Float accuracy = newLoc.getAccuracy();
        boolean bChanged = false;
        if ((longitude!=null)&&(latitude!=null)) {
            if ((!longitude.equals(lastLon))||(!latitude.equals(lastLat))) {
                bChanged = true;
                lastLon = longitude;
                lastLat = latitude;
            }
        }
        if (bChanged) {
            MetroStation closestStationRaw = getClosestStation(lastLon, lastLat, null);
            MetroStation closeStationRaw = getClosestStation(lastLon, lastLat, closestStationRaw);
            TransportStop closestStopRaw = getClosestStop(lastLon, lastLat, null);
            TransportStop closeStopRaw = getClosestStop(lastLon, lastLat, closestStopRaw);
            boolean bNearMetro = isNearClosestStation(lastLon, lastLat, closestStationRaw, closeStationRaw);
            MetroStation closestStation = null;
            if (bNearMetro) closestStation = closestStationRaw;
            boolean bNearStop = isNearClosestStop(lastLon, lastLat, closestStopRaw, closeStopRaw);
            TransportStop closestStop = null;
            if (bNearStop) closestStop = closestStopRaw;
            boolean bNearObject = bNearMetro || bNearStop;
            if (bNearObject) {
                boolean bSameStation = false;
                if ((lastStation == null) && (closestStation == null)) bSameStation = true;
                if ((!bSameStation) && (lastStation != null) && (closestStation != null))
                    bSameStation = lastStation.equals(closestStation);
                boolean bSameStop = false;
                if ((lastStop == null) && (closestStop == null)) bSameStop = true;
                if ((!bSameStop) && (lastStop != null) && (closestStop != null))
                    bSameStop = lastStop.equals(closestStop);
                boolean bNearObjectChanged = (!bSameStation) || (!bSameStop);
                if (bNearObjectChanged) {
                    if (!bSameStation) setLastStation(lastLon, lastLat, closestStation);
                    if (!bSameStop) setLastStop(lastLon, lastLat, closestStop);
                    near1signalled = false;
                    near2signalled = false;
                    updateDistance(lastLon, lastLat);
                    checkSingnalled(bSameStation, bSameStop);
                } else {
                    updateDistance(lastLon, lastLat);
                    checkSingnalled(false, false);
                }
            }
        }
    }

    public static MetroStation getPrevNextStation(MetroStation ms, boolean isPrev) {
        if (ms != null) {
            for (MetroLocation ml: metroLocations)
                for (MetroLine mln: ml.metroLines)
                    for (int i=0; i < mln.metroStations.size(); i++) {
                        if (ms.equals(mln.metroStations.get(i))) {
                            if ((isPrev) && (i>0)) return mln.metroStations.get(i-1);
                            if ((!isPrev) && (i<mln.metroStations.size()-1)) return mln.metroStations.get(i+1);
                        }
                    }
        }
        return null;
    }

    public static String getStationHexColor(MetroStation ms) {
        if (ms != null) {
            for (MetroLocation ml: metroLocations)
                for (MetroLine mln: ml.metroLines)
                    for (MetroStation mst: mln.metroStations) {
                        if (ms.equals(mst)) return mln.hexColor;
                    }
        }
        return null;
    }

}
