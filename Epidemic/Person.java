import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * People occupy places
 * @author Allison Ryan
 * @version 12/4/2020
 * @see Place
 * @see Employee
 * @see Simulator
 */
public class Person {
    // private stuff needed for instances

    protected enum States {
        uninfected, latent, infectious, bedridden, recovered, dead
            // the order of the above is significant: >= uninfected is infected
    }

    // static attributes describing progression of infection
    double latentMedT = 2 * Simulator.day;
    double latentScatT = 1 * Simulator.day;
    double bedriddenProb = 0.7;
    double infectRecMedT = 1 * Simulator.week;
    double infectRecScatT = 6 * Simulator.day;
    double infectBedMedT = 3 * Simulator.day;
    double infectBedScatT = 5 * Simulator.day;
    double deathProb = 0.2;
    double bedRecMedT = 2 * Simulator.week;
    double bedRecScatT = 1 * Simulator.week;
    double bedDeadMedT = 1.5 * Simulator.week;
    double bedDeadScatT = 1 * Simulator.week;

    // static counts of infection progress
    private static int numUninfected = 0;
    private static int numLatent = 0;
    private static int numInfectious = 0;
    private static int numBedridden = 0;
    private static int numRecovered = 0;
    private static int numDead = 0;

    // fixed attributes of each instance
    private final HomePlace home;  // all people have homes
    public final String name;      // all people have names

    // instance variables
    protected Place place;         // when not in transit, where the person is
    public States infectionState;  // all people have infection states

    // the collection of all instances
    private static final LinkedList <Person> allPeople =
        new LinkedList <Person> ();

    // need a source of random numbers
    private static final MyRandom rand = MyRandom.stream();

    /** The only constructor
     *  @param h the home of the newly constructed person
     */
    public Person( HomePlace h ) {
        name = super.toString();
        home = h;
        place = h; // all people start out at home
        infectionState = States.uninfected;
        numUninfected = numUninfected + 1;
        h.addResident( this );

        allPeople.add( this ); // this is the only place items are added!
    }

    /** Predicate to test person for infectiousness
     *  @return true if the person can transmit infection
     */
    public boolean isInfectious() {
        return (infectionState == States.infectious)
            || (infectionState == States.bedridden);
    }

    /** Primarily for debugging
     *  @return textual name and home of this person
     */
    public String toString() {
        return name ;// DEBUG  + " " + home.name + " " + infectionState;
    }

    /** Shuffle the population
     *  This allows correlations between attributes of people to be broken
     */
    public static void shuffle() {
        Collections.shuffle( allPeople, rand );
    }

    /** Allow outsiders to iterate over all people
     *  @return an iterator over people
     */
    public static Iterator <Person> iterator() {
        return allPeople.iterator();
    }

    // simulation methods relating to infection process

    /** Infect a person
     *  @param t, the time at which the person is infected
     *  called when circumstances call for a person to become infected
     */
    public void infect( double t ) {
        if (infectionState == States.uninfected) {
            // infecting an already infected person has no effect
            double delay = rand.nextLogNormal( latentMedT, latentScatT );

            numUninfected = numUninfected - 1;
            infectionState = States.latent;
            numLatent = numLatent + 1;

            Simulator.schedule( new InfectEvent(delay + t, this) );
        }
    }

    /** An infected but latent person becomes infectous
     *  scheduled by infect() to make a latent person infectious
     */
    public void beInfectious( double t ) {
        numLatent = numLatent - 1;
        infectionState = States.infectious;
        numInfectious = numInfectious + 1;

        if (place != null) place.oneMoreInfectious( t );

        if (rand.nextFloat() > bedriddenProb) { // person stays asymptomatic
            double delay = rand.nextLogNormal( infectRecMedT, infectRecScatT );

            Simulator.schedule( new RecoverEvent(delay + t, this) );
        } else { // person becomes bedridden
            double delay = rand.nextLogNormal( infectBedMedT, infectBedScatT );

            Simulator.schedule( new BedridEvent(delay + t, this) );
        }
    }

    /** An infectious person becomes bedridden
     *  scheduled by beInfectious() to make an infectious person bedridden
     */
    protected void beBedridden( double t ) {
        numInfectious = numInfectious - 1;
        infectionState = States.bedridden;
        numBedridden = numBedridden + 1;

        if (rand.nextFloat() > deathProb) { // person recovers
            double delay = rand.nextLogNormal( bedRecMedT, bedRecScatT );

            Simulator.schedule( new RecoverEvent(delay + t, this) );
        } else { // person dies
            double delay = rand.nextLogNormal( bedDeadMedT, bedDeadScatT );

            Simulator.schedule( new DeadEvent(delay + t, this) );
        }

        // if in a place (not in transit) that is not home, go home now!
        if ((place != null) && (place != home)) goHome( t );
    }

    /** A infectious or bedridden person recovers
     *  scheduled by beInfectious() or beBedridden to make a person recover
     */
    protected void beRecovered( double time ) {
        if (infectionState == States.infectious) {
            numInfectious = numInfectious - 1;
        } else {
            numBedridden = numBedridden - 1;
        }
        infectionState = States.recovered;
        numRecovered = numRecovered + 1;

        if (place != null) place.oneLessInfectious( time );
    }

    /** A bedridden person dies
     *  scheduled by beInfectious() to make a bedridden person die
     */
    protected void beDead( double time ) {
        numBedridden = numBedridden - 1;
        infectionState = States.dead; // needed to prevent resurrection
        numDead = numDead + 1;

        // if the person died in a place, make them leave it!
        if (place != null) place.depart( this, time );

    }

    // simulation methods relating to daily reporting

    /** Make the daily midnight report
     *  @param t, the current time
     */
    public static void report( double t ) {
        System.out.println(
                "at " + t
                + ", un = " + numUninfected
                + ", lat = " + numLatent
                + ", inf = " + numInfectious
                + ", bed = " + numBedridden
                + ", rec = " + numRecovered
                + ", dead = " + numDead
                );

        // make this happen cyclically
        Simulator.schedule( new ReportEvent( t + Simulator.day ) );
    }

    // simulation methods relating to personal movement

    /** Make a person arrive at a new place
     *  @param p new place
     *  @param t the current time
     *  scheduled
     */
    protected void arriveAt( double time, Place p ) {
        if ((infectionState == States.bedridden) && (p != home)) {
            // go straight home if you arrive at work while sick
            goHome( time );

        } else if (infectionState == States.dead) { // died on the way to work
            // allow this person to be forgotten

        } else { // only really arrive if not sick
            p.arrive( this, time );
            this.place = p;
        }
    }

    /** Move a person to a new place
     *  @param p, the place where the person travels
     *  @param t, time at which the move will be completed
     */
    public void travelTo( Place p, double t ) {
        this.place = null;

        Simulator.schedule( new ArriveEvent(t, this, p) );
    }

    /** Simulate the trip home from wherever
     * @param time of departure
     */
    public void goHome( double time ) {
        double travelTime = rand.nextLogNormal(
                20 * Simulator.minute, // mean travel time
                3 * Simulator.minute   // scatter in travel time
                );

        // the possibility of arriving at work after falling ill requires this
        if (this.place != null) this.place.depart( this, time );

        this.travelTo( this.home, time + travelTime );
    }
}

// Helper classes for scheduling events in class Person

/**
 * Schedules infectious event
 * Called from Person.infect()
 * @author Allison Ryan
 * @version 12/4/2020
 * Status: Working
 * @see Person
 * @see Simulator
 */
class InfectEvent extends Simulator.Event {
    Person per;
    InfectEvent(double t, Person p) {
        super(t);
        per = p;
    }
    public void trigger() {
        per.beInfectious( time );
    }
}

/**
 * Schedules bedridden event
 * Called from Person.beInfectious()
 * @author Allison Ryan
 * @version 12/4/2020
 * Status: Working
 * @see Person
 * @see Simulator
 */
class BedridEvent extends Simulator.Event {
    Person per;
    BedridEvent(double t, Person p) {
        super(t);
        per = p;
    }
    public void trigger() {
        per.beBedridden( time );
    }
}

/**
 * Schedules recovery event
 * Called from Person.beInfectious() and Person.beBedridden()
 * @author Allison Ryan
 * @version 12/4/2020
 * Status: Working
 * @see Person
 * @see Simulator
 */
class RecoverEvent extends Simulator.Event {
    Person per;
    RecoverEvent(double t, Person p) {
        super(t);
        per = p;
    }
    public void trigger() {
        per.beRecovered( time );
    }
}

/**
 * Schedules death event
 * Called from Person.beDead()
 * @author Allison Ryan
 * @version 12/4/2020
 * Status: Working
 * @see Person
 * @see Simulator
 */
class DeadEvent extends Simulator.Event {
    Person per;
    DeadEvent(double t, Person p) {
        super(t);
        per = p;
    }
    public void trigger() {
        per.beDead( time );
    }
}

/**
 * Schedules arrival at place event
 * Called from Person.travelTo()
 * @author Allison Ryan
 * @version 12/4/2020
 * Status: Working
 * @see Person
 * @see Place
 * @see Simulator
 */
class ArriveEvent extends Simulator.Event {
    Person per;
    Place plc;
    ArriveEvent(double t, Person p, Place pl) {
        super(t);
        per = p;
        plc = pl;
    }
    public void trigger() {
        per.arriveAt( time, plc );
    }
}

/**
 * Schedules daily report event
 * Called from Person.report()
 * @author Allison Ryan
 * @version 12/4/2020
 * Status: Working
 * @see Person
 * @see Simulator
 */
class ReportEvent extends Simulator.Event {
    ReportEvent(double t) { super(t); }
    public void trigger() {
        Person.report( time );
    }
}
