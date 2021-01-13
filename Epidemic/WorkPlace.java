import java.util.LinkedList;

/**
 * WorkPlaces are occupied by employees
 * @author Allison Ryan
 * @version 12/4/2020
 * @see Place
 * @see Employee
 */
public class WorkPlace extends Place {
    private final LinkedList <Employee> employees = new LinkedList <Employee>();

    // transmissivity median and scatter for workplaces
    private static final double transMed = 0.02 * Simulator.hour;
    private static final double transScat = 0.25 * Simulator.hour;

    // need a source of random numbers
    private static final MyRandom rand = MyRandom.stream();

    /** The only constructor for WorkPlace
     *  WorkPlaces are constructed with no residents
     */
    public WorkPlace() {
	super(); // initialize the underlying place
	super.transmissivity = rand.nextLogNormal( transMed, transScat );

	// make the workplace open at 8 AM
	class MyEvent extends Simulator.Event {
	    MyEvent() { super( 8 * Simulator.hour ); }
	    public void trigger() {
		open( time );
	    }
	}
	Simulator.schedule( new MyEvent() );
    }

    /** Add an employee to a WorkPlace
     *  Should only be called from the person constructor
     *  @param r an Employee, the new worker
     */
    public void addEmployee( Employee r ) {
	employees.add( r );
	// no need to check to see if the person already works there?
    }

    /** Primarily for debugging
     * @return textual name and employees of the workplace
     */
    public String toString() {
	String res = name;
	// DEBUG for (Employee p: employees) { res = res + " " + p.name; }
	return res;
    }

    // simulation methods

    /** open the workplace for business
     *  @param t the time of day
     *  Note that this workplace will close itself 8 hours later, and
     *  opening plus closing should create a 24-hour cycle.
     *  @see close
     */
    private void open( double t ) {
	// DEBUG System.out.println( this.toString() + " opened at time " + time );

	// close this workplace 8 hours later
	class MyEvent extends Simulator.Event {
	    MyEvent() { super( t + 8 * Simulator.hour ); }
	    public void trigger() {
		close( time );
	    }
	}
	Simulator.schedule( new MyEvent() );
    }

    /** close the workplace for the day
     *  @param t the time of day
     *  note that this workplace will reopen 16 hours later, and
     *  opening plus closing should create a 24-hour cycle.
     *  @see open
     */
    private void close( double t ) {
	//System.out.println( this.toString() + " closed at time " + time );

	// open this workplace 16 hours later, with no attention to weekends
	class MyOpenEvent extends Simulator.Event {
	    MyOpenEvent() { super( t + 16 * Simulator.hour ); }
	    public void trigger() {
		open( time );
	    }
	}
	Simulator.schedule( new MyOpenEvent() );

	// send everyone home now
	for (Person p: occupants) {
	    // schedule it for now in order to avoid modifying list inside loop
	    // not doing this gives risk of ConcurrentModificationException
	    class MyHomeEvent extends Simulator.Event {
		MyHomeEvent() { super( t ); }
		public void trigger() {
		    p.goHome( time );
		}
	    }
	    Simulator.schedule( new MyHomeEvent() );
	}
    }
}
