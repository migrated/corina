package corina.cross;

import corina.Sample;
import corina.ui.I18n;
import corina.site.Site;
import corina.site.SiteDB;
import corina.site.SiteNotFoundException;

import java.text.DecimalFormat;

// a single crossdate between two samples, where they are.
// used by Grid, Table, and anybody else who wants to see if samples crossdate.

public class Single {
    // the scores themselves
    private float t, tr, d;
    // FUTURE: float[]

    // get rid of these formatters eventually.  scores should be able
    // to format themselves.
    static DecimalFormat f1, f2, f3;
    static {
        // REFACTOR: the crosses to use should be user-pickable, so this is B-A-D.
        f1 = new DecimalFormat(new TScore().getFormat());
        f2 = new DecimalFormat(new Trend().getFormat());
        f3 = new DecimalFormat(new DScore().getFormat());
    }

    // -- return all scores, formatted properly, in an array?
    // public String[] formatAll() ?

    public String formatT() {
	return f1.format(t);
    }
    public String formatTrend() {
	return f2.format(tr);
    }
    public String formatD() {
	return f3.format(d);
    }

    /*
      BETTER INTERFACE:
      -- float scores[]
      -- scores[i] is for ALL_CROSSDATES[i]
      -- String format(String alg)? -- single.format("TScore") => "1.23"
      REFACTORING:
      -- write formatT() = format("TScore"), etc.
      -- switch uses of format() to format("TScore")
      -- switch format(),format(),format() to loop through DEFAULT_CROSSDATES
    */

    public String toXML() {
	return "<cross t=\"" + t + "\" tr=\"" + tr + "\" d=\"" + d + "\" n=\"" + n + "\"/>";
	// REFACTOR: would messageformat be clearer?
    }
    // TODO: make Cross.getShortName() ("t", "tr", "D", etc.) -- who uses this?

    // FUTURE: use Cross.DEFAULT_CROSSDATES, OR: allow any number of algorithms here.

    // the overlap
    /*private*/ int n;

    // distance between sites, in km, or null if unknown
    /*private*/ Integer dist;

    // is it significant?
    /*private*/ boolean isSig;

    // run a single crossdate between 2 samples
    public Single(Sample fixed, Sample moving) {
	// fill in crosses, if they overlap
	n = fixed.range.overlap(moving.range);
	if (n > 0) {
	    // this use of single() is kind of hackish.  since it's only used here, it should be REFACTORED.
	    t = new TScore(fixed, moving).single();
	    tr = new Trend(fixed, moving).single();
	    d = new DScore(fixed, moving).single();
	} else {
	    t = tr = d = 0; // right?
	}

	// distance
	try {
	    Site s1 = SiteDB.getSiteDB().getSite(fixed);
	    Site s2 = SiteDB.getSiteDB().getSite(moving);
	    dist = new Integer(s1.distanceTo(s2));
	} catch (SiteNotFoundException snfe) {
	    dist = null;
	}

	// is it significant?  use the t-score to check.  (why t-score?  why just one?)
	isSig = new TScore().isSignificant(t, n);
    }

    // FOR BACKWARDS COMPATIBILITY ONLY -- REFACTOR AND REMOVE ME -- ??
    // -- used only for Grid loading
    public Single(float t, float tr, float d, int n) {
	this.t = t;
	this.tr = tr;
	this.d = d;
	this.n = n;
	isSig = new TScore().isSignificant(t, n);
    }

    public boolean isSignificant() {
	return isSig;
    }

    // as "x km", localized, or the empty string if unknown(null)
    public String distanceAsString() {
	if (dist == null)
	    return "";

	return dist + " " + I18n.getText("km");
    }
}
