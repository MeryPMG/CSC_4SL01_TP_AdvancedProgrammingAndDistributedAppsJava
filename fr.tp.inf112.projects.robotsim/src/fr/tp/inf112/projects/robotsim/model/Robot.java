package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {
	
	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

	private final Battery battery;
	
	private int speed;
	
	private List<Component> targetComponents;
	
	private transient Iterator<Component> targetComponentsIterator;
	
	private Component currTargetComponent;
	
	private transient Iterator<Position> currentPathPositionsIter;
	
	private transient boolean blocked;
	
	private Position blockedTargetPosition;
	
	private FactoryPathFinder pathFinder;

	public Robot(final Factory factory,
				 final FactoryPathFinder pathFinder,
				 final CircularShape shape,
				 final Battery battery,
				 final String name ) {
		super(factory, shape, name);
		
		this.pathFinder = pathFinder;
		
		this.battery = battery;
		
		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		setBlocked(false); // notify as soon something is changed
		blockedTargetPosition = null;
	}
	
	private void setBlocked(boolean blocked) {
		this.blocked = blocked;
		notifyObservers();
	}

	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	protected int getSpeed() {
		return speed;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}
	
	public Position getBlockedTargetPosition() {
		return blockedTargetPosition;
	}
	
	private List<Component> getTargetComponents() {
		if (targetComponents == null) {
			targetComponents = new ArrayList<>();
		}
		
		return targetComponents;
	}
	
	public boolean addTargetComponent(final Component targetComponent) {
		return getTargetComponents().add(targetComponent);
	}
	
	public boolean removeTargetComponent(final Component targetComponent) {
		return getTargetComponents().remove(targetComponent);
	}
	
	@Override
	public boolean isMobile() {
		return true;
	}
	
	public boolean isBlocked() {
		return blocked;
	}

	@Override
	public boolean behave() {
		if (getTargetComponents().isEmpty()) {
			return false;
		}
		
		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			currTargetComponent = nextTargetComponentToVisit();
			
			computePathToCurrentTargetComponent();
		}

		return moveToNextPathPosition() != 0;
	}
		
	private Component nextTargetComponentToVisit() {
		if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
			targetComponentsIterator = getTargetComponents().iterator();
		}
		
		return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
	}
	
	private int moveToNextPathPosition() {
		final int previousX = getxCoordinate();
	    final int previousY = getyCoordinate();
		
		final Motion motion = computeMotion();
		
		int displacement = motion == null ? 0 : motion.moveToTarget();
		
		if (displacement != 0) {
			getFactory().unlock(previousX, previousY);
			notifyObservers();
		} 
		else if (isLivelyLocked()) {
			final Position freeNeighbouringPosition = findFreeNeighbouringPosition();

			if (freeNeighbouringPosition != null) {
				blockedTargetPosition = freeNeighbouringPosition;
				displacement = moveToNextPathPosition();
				computePathToCurrentTargetComponent();
			}
		}
		
		return displacement;
	}
	
	private void computePathToCurrentTargetComponent() {
		final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
	}
	
	private Motion computeMotion() {
		if (!currentPathPositionsIter.hasNext() && blockedTargetPosition == null) {

			// There is no free path to the target
			setBlocked(true);
			
			return null;
		}
		
		
		final Position targetPosition = getTargetPosition();
		
		// If there is another robot, memorize the blocked target position for the next run
		if (!getFactory().tryGetLockAtPosition(targetPosition.getxCoordinate(), targetPosition.getyCoordinate())) {
			this.blockedTargetPosition = targetPosition;
			
			// See if that other robot is permanently blocked and will not move from the target position
			final Component otherComponent = getFactory().getMobileComponentAt(targetPosition, this);

			if (otherComponent instanceof Robot) {
				final Robot otherRobot = (Robot) otherComponent;

				if (otherRobot.isBlocked()) {
					setBlocked(true);
				}
			}
			return null;
		}
		
		// Reset the memorized position
		this.blockedTargetPosition = null;
		return new Motion(getPosition(), targetPosition);
	}
	
	private Position getTargetPosition() {
		// If a target position was memorized, it means that the robot was blocked during the last iteration 
		// so it waited for another robot to pass. So try to move to this memorized position otherwise move to  
		// the next position from the path
		return this.blockedTargetPosition == null ? currentPathPositionsIter.next() : this.blockedTargetPosition;
	}
	
	private boolean hasReachedCurrentTarget() {
		return getPositionedShape().overlays(currTargetComponent.getPositionedShape());
	}
	
	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}
	
	@Override
	public Style getStyle() {
		return blocked ? BLOCKED_STYLE : STYLE;
	}
	
	public boolean isLivelyLocked() { 
	    final Position blockedTargetPosition = getBlockedTargetPosition(); 
	   
	    if (blockedTargetPosition == null) { 
	        return false; 
	    } 
	   
	    final Component otherRobot =     
	        getFactory().getMobileComponentAt(blockedTargetPosition, this);                                                       
	 
	    return otherRobot != null && 
	           getPosition().equals(((Robot) otherRobot).getBlockedTargetPosition()); 
	}
	
	public Position findFreeNeighbouringPosition() {
		final Position currentPosition = getPosition();
		final int resolution = getFactory().getPathResolution();
		
		Position attemptNewPosition = null;
			
		if (this.blockedTargetPosition.getxCoordinate() == currentPosition.getxCoordinate()) {
			attemptNewPosition = new Position(currentPosition.getxCoordinate() + resolution, currentPosition.getyCoordinate());
		} else if (this.blockedTargetPosition.getyCoordinate() == currentPosition.getyCoordinate()) {
			attemptNewPosition = new Position(currentPosition.getxCoordinate(), currentPosition.getyCoordinate() + resolution);
		}
		
		PositionedShape shape = new RectangularShape(attemptNewPosition.getxCoordinate(),
														   attemptNewPosition.getyCoordinate(),
														   2,
														   2);

		if (!getFactory().hasMobileComponentAt(shape, this)) {
			return attemptNewPosition;
		}
		
		
		if (this.blockedTargetPosition.getxCoordinate() == currentPosition.getxCoordinate()) {
			attemptNewPosition = new Position(currentPosition.getxCoordinate() - resolution, currentPosition.getyCoordinate());
		} else if (this.blockedTargetPosition.getyCoordinate() == currentPosition.getyCoordinate()) {
			attemptNewPosition = new Position(currentPosition.getxCoordinate(), currentPosition.getyCoordinate() - resolution);
		}
		
		shape = new RectangularShape(attemptNewPosition.getxCoordinate(),
				   attemptNewPosition.getyCoordinate(),
				   2,
				   2);
		
		if (!getFactory().hasMobileComponentAt(shape, this)) {
			return attemptNewPosition;
		}
		
		return null;
	}
	
	@Override
	public void run() {
		Position initialPosition = getPosition();

		if(!getFactory().tryGetLockAtPosition(initialPosition.getxCoordinate(), initialPosition.getyCoordinate())) {
			System.err.println("Error: the starting position of two robots is the same");
		} else {
			try {
				while(isSimulationStarted()) {
					behave();
					
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException ex) {
						System.err.println("Symulation was abruptly interrupted");
					}
				}
			} finally {
				Position currentPosition = getPosition();
				getFactory().unlock(currentPosition.getxCoordinate(), currentPosition.getyCoordinate());
			}
		}
	}
}
