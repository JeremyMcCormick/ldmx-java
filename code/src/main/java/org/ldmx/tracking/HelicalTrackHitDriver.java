package org.ldmx.tracking;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.axial.HelicalTrack2DHit;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

/**
 * Copied from {@link org.hps.recon.tracking.HelicalTrackerHitDriver} in HPS Java so it 
 * can be hacked up.
 */
public class HelicalTrackHitDriver extends org.lcsim.fit.helicaltrack.HelicalTrackHitDriver {

    private boolean _debug = false;
    private double _clusterTimeCut = -99; // if negative, don't cut..otherwise,
    // dt cut time in ns
    private double maxDt = -99; // max time difference between the two hits in a cross
    private double clusterAmplitudeCut = -99; // cluster amplitude cut
    private String _subdetectorName = "Tracker";
    private final Map<String, String> _stereomap = new HashMap<String, String>();
    private List<SvtStereoLayer> stereoLayers = null;
    private List<HpsSiSensor> axialOnlySensors = null;
    private final List<String> _colnames = new ArrayList<String>();
    private boolean _doTransformToTracking = true;
    private boolean _saveAxialHits = false;
    private final String _axialname = "AxialTrackHits";
    private final String _axialmcrelname = "AxialTrackHitsMCRelations";
    private List<Integer> stereoLayerList = null;
    private List<Integer> axialLayerList = null;
    
    private Map<Integer, List<HpsSiSensor>> sensorLayerMap = new HashMap<Integer, List<HpsSiSensor>>();

    /**
     * Default Ctor
     */
    public HelicalTrackHitDriver() {
        _crosser.setMaxSeparation(20.0);
        _crosser.setTolerance(0.1);
        _crosser.setEpsParallel(0.013);
        _colnames.add("StripClusterer_SiTrackerHitStrip1D");
    }
    
    public void setStereoLayers(int[] stereoLayers) {
        if (stereoLayers == null) {
            throw new IllegalArgumentException("The pairs array points to null.");
        }
        if (stereoLayers.length == 0) {
            throw new IllegalArgumentException("The pairs array is empty.");
        }
        if (stereoLayers.length < 2) {
            throw new IllegalArgumentException("The pairs array does not have enough data.");
        }
        if (stereoLayers.length % 2 != 0) {
            throw new IllegalArgumentException("The pairs array does not have an even number of elements.");
        }
        for (int i = 0; i < stereoLayers.length - 1; i += 2) {
            System.out.println("axial lyr " + stereoLayers[i] + " will be paired with stereo lyr " + stereoLayers[i + 1]);
        }
        stereoLayerList = new ArrayList<Integer>();
        for (int stereoLayer : stereoLayers) {
            stereoLayerList.add(stereoLayer);
        }
    }
    
    public void setAxialOnlyLayers(int[] axialOnlyLayers) {
        this.axialLayerList = new ArrayList<Integer>();
        for (int axialLayer : axialOnlyLayers) {
            axialLayerList.add(axialLayer);
        }
    }

    /**
     *
     * @param dtCut
     */
    public void setClusterTimeCut(double dtCut) {
        this._clusterTimeCut = dtCut;
    }

    public void setMaxDt(double maxDt) {
        this.maxDt = maxDt;
    }

    public void setClusterAmplitudeCut(double clusterAmplitudeCut) {
        this.clusterAmplitudeCut = clusterAmplitudeCut;
    }

    /**
     *
     * @param subdetectorName
     */
    public void setSubdetectorName(String subdetectorName) {
        this._subdetectorName = subdetectorName;
    }

    /**
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public void setEpsParallel(double eps) {
        this._crosser.setEpsParallel(eps);
    }

    public void setEpsStereo(double eps) {
        this._crosser.setEpsStereoAngle(eps);
    }

    /**
     *
     * @param trans
     */
    public void setTransformToTracking(boolean trans) {
        this._doTransformToTracking = trans;
    }

    public void setSaveAxialHits(boolean save) {
        _saveAxialHits = save;
    }

    /**
     *
     * @param stripHitsCollectionName
     */
    public void setStripHitsCollectionName(String stripHitsCollectionName) {
        HitRelationName(stripHitsCollectionName);
    }

    /**
     *
     * @param helicalTrackHitRelationsCollectionName
     */
    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        HitRelationName(helicalTrackHitRelationsCollectionName);
    }

    /**
     *
     * @param helicalTrackMCRelationsCollectionName
     */
    public void setHelicalTrackMCRelationsCollectionName(String helicalTrackMCRelationsCollectionName) {
        MCRelationName(helicalTrackMCRelationsCollectionName);
    }

    /**
     *
     * @param outputHitCollectionName
     */
    public void setOutputHitCollectionName(String outputHitCollectionName) {
        OutputCollection(outputHitCollectionName);
    }
    
    private Map<Integer, List<HpsSiSensor>> makeSensorMap(List<HpsSiSensor> sensors) {
        Map<Integer, List<HpsSiSensor>> sensorMap = new HashMap<Integer, List<HpsSiSensor>>();
        for (HpsSiSensor sensor : sensors) {
            final int sensorLayer = sensor.getLayerNumber();
            if (!sensorMap.containsKey(sensorLayer)) {
                sensorMap.put(sensorLayer, new ArrayList<HpsSiSensor>());
            }
            sensorMap.get(sensorLayer).add(sensor);
        }        
        return sensorMap;
    }

    @Override
    public void process(EventHeader event) {

        // Instantiate the list of HelicalTrackCrosses and HelicalTrackHits
        List<HelicalTrackCross> stereoCrosses = new ArrayList<HelicalTrackCross>();
        List<HelicalTrackHit> helhits = new ArrayList<HelicalTrackHit>();
        
        // Create an LCRelation from a HelicalTrackHit to
        List<LCRelation> hitrelations = new ArrayList<LCRelation>();
        
        // Create an LCRelation from a HelicalTrackHit to an MC particle used to
        // create it
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        RelationalTable hittomc = createHelicalTrackHitMCRelations(event);

        List<HelicalTrack2DHit> axialhits = new ArrayList<>();
        List<LCRelation> axialmcrelations = new ArrayList<LCRelation>();
        
        // Loop over the input collection names we want to make hits out of
        // ...for HPS, probably this is just a single one...
        for (String _colname : this._colnames) {
            if (!event.hasCollection(SiTrackerHit.class, _colname)) {
                if (_debug) {
                    System.out.println("Event: " + event.getRunNumber() + " does not contain the collection " + _colname);
                }
                continue;
            }
            // Get the list of SiTrackerHits for this collection
            List<SiTrackerHit> hitlist = event.get(SiTrackerHit.class, _colname);
            if (_debug) {
                System.out.printf("%s: found %d SiTrackerHits\n", this.getClass().getSimpleName(), hitlist.size());
            }
            Map<HelicalTrackStrip, SiTrackerHitStrip1D> stripmap = new HashMap<HelicalTrackStrip, SiTrackerHitStrip1D>();
            for (SiTrackerHit hit : hitlist) {
                if (hit instanceof SiTrackerHitStrip1D) {

                    // Cast the hit as a 1D strip hit and find the
                    // identifier for the detector/layer combo
                    SiTrackerHitStrip1D h = (SiTrackerHitStrip1D) hit;
                    if (clusterAmplitudeCut > 0 && h.getdEdx() / DopedSilicon.ENERGY_EHPAIR < clusterAmplitudeCut) {
                        System.out.println("cut on cluster amplitude");
                        continue;
                    }
                    if (_clusterTimeCut > 0 && Math.abs(h.getTime()) > _clusterTimeCut) {
                        System.out.println("cut on cluster time");
                        continue;
                    }

                    // Create a HelicalTrackStrip for this hit
                    HelicalTrackStrip strip = makeDigiStrip(h);
                    if (hittomc != null) {
                        for (RawTrackerHit rth : h.getRawHits()) {
                            for (Object simHit : hittomc.allFrom(rth)) {
                                strip.addMCParticle(((SimTrackerHit) simHit).getMCParticle());
                            }
                        }
                    }

                    // Map a reference back to the hit needed to create
                    // the stereo hit LC relations
                    stripmap.put(strip, h);
                    if (_debug) {
                        System.out.printf("%s: added strip org %s layer %d\n", this.getClass().getSimpleName(), strip.origin().toString(), strip.layer());
                    }
                                                           
                    // handle hits on axial only layers
                    if (this.axialOnlySensors.contains(h.getSensor())) {
                        HelicalTrack2DHit haxial = makeDigiAxialHit(h);
                        axialhits.add(haxial);
                        if (hittomc != null) {
                            List<RawTrackerHit> rl = haxial.getRawHits();
                            for (RawTrackerHit rth : rl) {
                                for (Object simHit : hittomc.allFrom(rth)) {
                                    haxial.addMCParticle(((SimTrackerHit) simHit).getMCParticle());
                                }
                            }
                        }
                        axialmcrelations.add(new MyLCRelation(haxial, haxial.getMCParticles()));
                    }
                } 
            }

            List<HelicalTrackCross> helicalTrackCrosses = new ArrayList<HelicalTrackCross>();

            // Create collections for strip hits by layer and hit cross
            // references
            Map<String, List<HelicalTrackStrip>> striplistmap = new HashMap<String, List<HelicalTrackStrip>>();

            for (HelicalTrackStrip strip : stripmap.keySet()) {
                
                IDetectorElement de = stripmap.get(strip).getSensor();
                String id = this.makeID(_ID.getName(de), _ID.getLayer(de));

                // This hit should be a on a stereo pair!
                // With our detector setup, when is this not true?
                if (!_stereomap.containsKey(id) && !_stereomap.containsValue(id)) {
                    System.out.println("skipping hit not from a stereo layer!");
                    //throw new RuntimeException(this.getClass().getSimpleName() + ": this " + id
                    //        + " was not among the stereo modules!");
                }

                // Get the list of strips for this layer - create a new
                // list if one doesn't already exist
                List<HelicalTrackStrip> lyrhits = striplistmap.get(id);
                if (lyrhits == null) {
                    lyrhits = new ArrayList<HelicalTrackStrip>();
                    striplistmap.put(id, lyrhits);
                }

                // Add the strip to the list of strips on this
                // sensor
                lyrhits.add(strip);
            }

            if (_debug) {
                System.out.printf("%s: Create stereo hits from %d strips \n", this.getClass().getSimpleName(),
                        striplistmap.size());
            }

            // Loop over the stereo layer pairs
            // TODO: Change this so that it makes use of StereoPairs
            for (String id1 : _stereomap.keySet()) {
                
                // Get the second layer
                String id2 = _stereomap.get(id1);

                if (_debug) {
                    System.out.printf("%s: Form stereo hits from sensor id %s with %d hits and %s with %d hits\n", this
                            .getClass().getSimpleName(), id1, striplistmap.get(id1) == null ? 0 : striplistmap.get(id1)
                            .size(), id2, striplistmap.get(id2) == null ? 0 : striplistmap.get(id2).size());
                }

                // Form the stereo hits and add them to our hit list
                helicalTrackCrosses.addAll(_crosser.MakeHits(striplistmap.get(id1), striplistmap.get(id2)));
            } // End of loop over stereo pairs

            for (Iterator<HelicalTrackCross> iter = helicalTrackCrosses.listIterator(); iter.hasNext();) {
                HelicalTrackCross cross = iter.next();
                if (maxDt > 0 && Math.abs(cross.getStrips().get(0).time() - cross.getStrips().get(1).time()) > maxDt) {
                    iter.remove();
                    continue;
                }
                if (cross.getMCParticles() != null) {
                    for (MCParticle mcp : cross.getMCParticles()) {
                        mcrelations.add(new MyLCRelation((HelicalTrackHit) cross, mcp));
                    }
                }
                for (HelicalTrackStrip strip : cross.getStrips()) {
                    hitrelations.add(new MyLCRelation(cross, stripmap.get(strip)));
                }
                if (_debug) {
                    System.out.printf("%s: cross at %.2f,%.2f,%.2f \n", this.getClass().getSimpleName(), cross.getPosition()[0], cross.getPosition()[1], cross.getPosition()[2]);
                }
            }

            stereoCrosses.addAll(helicalTrackCrosses);

            if (_debug) {
                System.out.printf("%s: added %d stereo hits from %s collection \n", this.getClass().getSimpleName(), helicalTrackCrosses.size(), _colname);
            }
        } // End of loop over collection names

        if (_debug) {
            System.out.printf("%s: totally added %d stereo hits:\n", this.getClass().getSimpleName(), stereoCrosses.size());
            for (HelicalTrackCross cross : stereoCrosses) {
                System.out.printf("%s: %.2f,%.2f,%.2f \n", this.getClass().getSimpleName(), cross.getPosition()[0], cross.getPosition()[1], cross.getPosition()[2]);
            }
        }

        // Add things to the event
        // Cast crosses to HTH
        helhits.addAll(stereoCrosses);

        // add all axial only hits to output collection --JM
        helhits.addAll(axialhits);
        
        event.put(_outname, helhits, HelicalTrackHit.class, 0);
        event.put(_hitrelname, hitrelations, LCRelation.class, 0);
        if (hittomc != null) {
            event.put(_mcrelname, mcrelations, LCRelation.class, 0);
        }
        
        /*
        if (_saveAxialHits) {
            event.put(_axialname, axialhits, HelicalTrackHit.class, 0);
            if (hittomc != null) {
                event.put(_axialmcrelname, axialmcrelations, LCRelation.class, 0);
                System.out.println(this.getClass().getSimpleName() + " : number of " + _axialmcrelname + " found = " + axialmcrelations.size());
            }
        }
        */
        if (_doTransformToTracking) {
            addRotatedHitsToEvent(event, stereoCrosses, hittomc != null);
            if (_saveAxialHits) {
                addRotated2DHitsToEvent(event, axialhits);
            }
        }
    } // Process()

    private RelationalTable createHelicalTrackHitMCRelations(EventHeader event) {
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        RelationalTable hittomc = null;
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            hittomc = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    hittomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        return hittomc;
    }
    
    public void addCollection(String colname) {
        _colnames.add(colname);
    }

    public void setCollection(String colname) {
        _colnames.clear();
        this.addCollection(colname);
    }

    private String makeID(String detname, int lyr) {
        return detname + lyr;
    }

    public void setStereoPair(String detname, int lyr1, int lyr2) {
        this._stereomap.put(this.makeID(detname, lyr1), this.makeID(detname, lyr2));
    }
    
    /**
     * Defines stereo layer pairings.
     * 
     * @param detector the detector geometry containing the tracker
     */
    private void defineLayers(Detector detector) {
        
        HpsTracker2 subdetDe = ((HpsTracker2) detector.getSubdetector(this._subdetectorName).getDetectorElement());
        List<HpsSiSensor> sensors = subdetDe.findDescendants(HpsSiSensor.class);
                
        // map sensors to their geometric layers for quick lookup
        this.sensorLayerMap = this.makeSensorMap(sensors);

        // list for stereo layers
        this.stereoLayers = new ArrayList<SvtStereoLayer>();
        
        if (this.stereoLayerList != null && !this.stereoLayerList.isEmpty()) {
            int stereoLayerNumber = 1;
            for (int i = 0; i < (stereoLayerList.size() - 1); i += 2) {
                
                int axialLayer = stereoLayerList.get(i);
                int stereoLayer = stereoLayerList.get(i + 1);
                
                // HACK: make sure axial sensor is correctly assigned
                HpsSiSensor axialSensor = this.sensorLayerMap.get(axialLayer).get(0);
                axialSensor.setAxial(true);
                
                // HACK: make sure stereo sensor is correctly assigned
                HpsSiSensor stereoSensor = this.sensorLayerMap.get(stereoLayer).get(0);
                stereoSensor.setStereo(true);
                
                // add stereo layer to list
                SvtStereoLayer svtStereoLayer = new SvtStereoLayer(stereoLayerNumber, axialSensor, stereoSensor);                
                this.stereoLayers.add(svtStereoLayer);
                
                System.out.println("added stereo layer " + stereoLayerNumber + "; axial sens: " 
                        + svtStereoLayer.getAxialSensor().getName() + "; stereo sens: " 
                        + svtStereoLayer.getStereoSensor().getName());
                
                // set stereo pair mapping
                this.setStereoPair(this._subdetectorName, axialLayer, stereoLayer);
                
                // increment stereo layer number
                stereoLayerNumber++;
            }
        } else {
            throw new RuntimeException("Missing valid stereo layer list.");
        }
        
        // now define axial only sensors
        this.axialOnlySensors = new ArrayList<HpsSiSensor>();
        if (this.axialLayerList != null && !this.axialLayerList.isEmpty()) {
            for (Integer axialLayer : this.axialLayerList) {
                System.out.println("adding " + sensorLayerMap.get(axialLayer).size() + " sensors from lyr " + axialLayer + " to axial sensor list");
                this.axialOnlySensors.addAll(sensorLayerMap.get(axialLayer));
            }
        } else {
            System.err.println("WARNING: No axial only sensors were defined!");
        }
    }

    @Override
    protected void detectorChanged(Detector detector) {

        // Get the collection of stereo layers from the detector
        //stereoLayers = ((HpsTracker2) detector.getSubdetector(this._subdetectorName).getDetectorElement()).getStereoPairs();
        
        defineLayers(detector); 
    }

    /*
     *  Make  HelicalTrack2DHits from SiTrackerHitStrip1D...note that these HelicalTrack2DHits
     *  are defined in org.hps.recon.tracking.axial (not the lcsim class)
     */
    private HelicalTrack2DHit makeDigiAxialHit(SiTrackerHitStrip1D h) {

        double z1 = h.getHitSegment().getEndPoint().x();
        double z2 = h.getHitSegment().getStartPoint().x();//x is the non-bend direction in the JLAB frame
        double zmin = Math.min(z1, z2);
        double zmax = Math.max(z1, z2);
        IDetectorElement de = h.getSensor();

        HelicalTrack2DHit hit = new HelicalTrack2DHit(h.getPositionAsVector(),
                h.getCovarianceAsMatrix(), h.getdEdx(), h.getTime(),
                h.getRawHits(), _ID.getName(de), _ID.getLayer(de),
                _ID.getBarrelEndcapFlag(de), zmin, zmax, h.getUnmeasuredCoordinate());

        return hit;
    }

    private HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {

        SiTrackerHitStrip1D local = h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(_orgloc);
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + ": makeDigiStrip: org " + org.toString() + " and u " + u.toString() + " v " + v.toString());
        }

        double umeas = local.getPosition()[0];
        double vmin = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getStartPoint());
        double vmax = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getEndPoint());
        double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

        IDetectorElement de = h.getSensor();
        String det = _ID.getName(de);
        int lyr = _ID.getLayer(de);
        BarrelEndcapFlag be = _ID.getBarrelEndcapFlag(de);

        double dEdx = h.getdEdx();
        double time = h.getTime();
        List<RawTrackerHit> rawhits = h.getRawHits();
        HelicalTrackStrip strip = new HelicalTrackStrip(org, u, v, umeas, du, vmin, vmax, dEdx, time, rawhits, det, lyr, be);

        try {
            if (h.getMCParticles() != null) {
                for (MCParticle p : h.getMCParticles()) {
                    strip.addMCParticle(p);
                }
            }
        } catch (RuntimeException e) {
            // Okay when MC info not present.
        }

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + ": makeDigiStrip: produced HelicalTrackStrip with origin " + strip.origin().toString() + " and u " + strip.u().toString() + " v " + strip.v().toString() + " w " + strip.w().toString());
        }

        return strip;
    }

    private void addRotatedHitsToEvent(EventHeader event, List<HelicalTrackCross> stereohits, boolean isMC) {

        List<HelicalTrackHit> rotatedhits = new ArrayList<HelicalTrackHit>();
        List<LCRelation> hthrelations = new ArrayList<LCRelation>();
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        for (HelicalTrackCross cross : stereohits) {
            List<HelicalTrackStrip> rotatedstriphits = new ArrayList<HelicalTrackStrip>();
            for (HelicalTrackStrip strip : cross.getStrips()) {

                Hep3Vector origin = strip.origin();
                Hep3Vector u = strip.u();
                Hep3Vector v = strip.v();
                double umeas = strip.umeas();
                double du = strip.du();
                double vmin = strip.vmin();
                double vmax = strip.vmax();
                double dedx = strip.dEdx();
                double time = strip.time();
                List<RawTrackerHit> rthList = strip.rawhits();
                String detname = strip.detector();
                int layer = strip.layer();
                BarrelEndcapFlag bec = strip.BarrelEndcapFlag();
                Hep3Vector neworigin = CoordinateTransformations.transformVectorToTracking(origin);
                Hep3Vector newu = CoordinateTransformations.transformVectorToTracking(u);
                Hep3Vector newv = CoordinateTransformations.transformVectorToTracking(v);
                HelicalTrackStrip newstrip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dedx, time, rthList, detname, layer, bec);
                for (MCParticle p : strip.MCParticles()) {
                    newstrip.addMCParticle(p);
                }
                rotatedstriphits.add(newstrip);
                if (_debug) {
                    System.out.printf("%s: adding rotated strip with origin %s and u %s v %s w %s \n", getClass().toString(), newstrip.origin().toString(), newstrip.u().toString(), newstrip.v().toString(), newstrip.w().toString());
                }
            }
            List<HelicalTrackStrip> strip1 = new ArrayList<HelicalTrackStrip>();
            List<HelicalTrackStrip> strip2 = new ArrayList<HelicalTrackStrip>();
            strip1.add(rotatedstriphits.get(0));
            strip2.add(rotatedstriphits.get(1));
            List<HelicalTrackCross> newhits = _crosser.MakeHits(strip1, strip2);
            if (newhits.size() != 1) {
                throw new RuntimeException("no rotated cross was created!?");
            }
            HelicalTrackCross newhit = newhits.get(0);
            //HelicalTrackCross newhit = new HelicalTrackCross(rotatedstriphits.get(0), rotatedstriphits.get(1));
            for (MCParticle mcp : cross.getMCParticles()) {
                newhit.addMCParticle(mcp);
            }
            rotatedhits.add(newhit);
            hthrelations.add(new MyLCRelation(cross, newhit));
            for (MCParticle mcp : newhit.getMCParticles()) {
                mcrelations.add(new MyLCRelation(newhit, mcp));
            }
        }

        event.put("Rotated" + _outname, rotatedhits, HelicalTrackHit.class, 0);
        event.put("Rotated" + _hitrelname, hthrelations, LCRelation.class, 0);
        if (isMC) {
            event.put("Rotated" + _mcrelname, mcrelations, LCRelation.class, 0);
        }
    }

    /*
     *  Rotate the 2D tracker hits
     */
    private void addRotated2DHitsToEvent(EventHeader event, List<HelicalTrack2DHit> striphits) {
        List<HelicalTrack2DHit> rotatedhits = new ArrayList<HelicalTrack2DHit>();
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        for (HelicalTrack2DHit twodhit : striphits) {
            Hep3Vector pos = new BasicHep3Vector(twodhit.getPosition());
            SymmetricMatrix cov = twodhit.getCorrectedCovMatrix();
            double dedx = twodhit.getdEdx();
            double time = twodhit.getTime();
            List<RawTrackerHit> rthList = twodhit.getRawHits();
            String detname = twodhit.Detector();
            int layer = twodhit.Layer();
            BarrelEndcapFlag bec = twodhit.BarrelEndcapFlag();
            double vmin = twodhit.axmin();
            double vmax = twodhit.axmax();
            Hep3Vector axDir = twodhit.axialDirection();
            Hep3Vector newpos = CoordinateTransformations.transformVectorToTracking(pos);
            Hep3Vector newaxdir = CoordinateTransformations.transformVectorToTracking(axDir);
            SymmetricMatrix newcov = CoordinateTransformations.transformCovarianceToTracking(cov);
            HelicalTrack2DHit newhit = new HelicalTrack2DHit(newpos, newcov, dedx, time, rthList, detname, layer, bec, vmin, vmax, newaxdir);
            for (MCParticle mcp : twodhit.getMCParticles()) {
                newhit.addMCParticle(mcp);
            }
            rotatedhits.add(newhit);
            for (MCParticle mcp : newhit.getMCParticles()) {
                mcrelations.add(new MyLCRelation(newhit, mcp));
            }
        }
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + ": " + _axialname + " size = " + rotatedhits.size());
            System.out.println(this.getClass().getSimpleName() + ": " + _axialmcrelname + " size = " + mcrelations.size());
        }
        event.put("Rotated" + _axialname, rotatedhits, HelicalTrackHit.class, 0);
        event.put("Rotated" + _axialmcrelname, mcrelations, LCRelation.class, 0);
    }

    public void saveAxial2DHits(boolean saveThem) {
        _saveAxialHits = saveThem;
    }
}
