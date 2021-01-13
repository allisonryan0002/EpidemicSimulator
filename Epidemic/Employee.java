/**
 * Employees are People who work
 * @author Allison Ryan
 * @version 12/4/2020
 * @see Person
 * @see WorkPlace
 */
public class Employee extends Person {
    // instance variables
    private WorkPlace job;  // employees have WorkPlaces
			    // can't be final because set post constructor

    // need a source of random numbers
    private static final MyRandom rand = MyRandom.stream();

    /** The only constructor
     *  @param h the HomePlace of the newly constructed Employee
     *  Note that employees are created without well-defined workplaces
     */
    public Employee( HomePlace h ) {
	super( h ); // construct the base person
	job = null;

	// go to work every day at 25 minutes before 8 AM
	class MyEvent extends Simulator.Event {
	    MyEvent() {
		super( (8 * Simulator.hour) - (25 * Simulator.minute) );
	    }
	    public void trigger() {
		goToWork( time );
	    }
	}
	Simulator.schedule( new MyEvent() );
    }

    /** Set workplace of employee
     *  @param w the workPlace of the newly constructed Employee
     *  No employee's workplace may be set more than once
     */
    public void setWorkplace( WorkPlace w ) {
	assert job == null;
	job = w;
	w.addEmployee( this );
    }

    /** Primarily for debugging
     * @return textual name home and employer of this person
     */
    public String toString() {
	return super.toString() ;// DEBUG + " " + job.name;
    }

    // simulation methods

    /** Simulate the daily trip to work
     * @param t, the time of departure
     */
    private void goToWork( double t ) {
	if (infectionState == States.dead) return; // finish killing the dead!

	// people only leave home if feeling OK
	if (infectionState != States.bedridden) {
	    double travelTime = rand.nextLogNormal(
		20 * Simulator.minute, // mean travel time
		3 * Simulator.minute   // scatter in travel time
	    );
	    this.place.depart( this, t );
	    this.travelTo( this.job, t + travelTime );
	}

	// go to work every day at the same time
	class MyEvent extends Simulator.Event {
	    MyEvent() { super( t + Simulator.day ); }
	    public void trigger() {
		goToWork( time );
	    }
	}
	Simulator.schedule( new MyEvent() );
    }
}
