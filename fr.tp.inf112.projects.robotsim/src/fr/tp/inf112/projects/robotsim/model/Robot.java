package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.CustomDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.JGraphTDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {
	
	private static final Logger LOGGER = Logger.getLogger(Robot.class.getName());
	
	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

	private static final Style TEMP_BLOCKED_STYLE = new ComponentStyle(RGBColor.ORANGE, RGBColor.BLACK, 3.0f, new float[]{4.0f});
	
	private final Battery battery;
	
	private int speed;
	
	private List<Component> targetComponents;
	
	@JsonIgnore
	private transient Iterator<Component> targetComponentsIterator;
	
	private Component currTargetComponent;
	
	@JsonIgnore
	private transient Iterator<Position> currentPathPositionsIter;
	
	private transient boolean blocked;
	
	private transient boolean temporarilyBlocked;
	
	private Position blockedTargetPosition;
	
	@JsonIgnore
	private FactoryPathFinder pathFinder;
	
	private String pathFinderType;

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
		setTemporarilyBlocked(false);
		blockedTargetPosition = null;
		
		if (pathFinder instanceof JGraphTDijkstraFactoryPathFinder) {
		    pathFinderType = "JGRAPH";
		} else if (pathFinder instanceof CustomDijkstraFactoryPathFinder) {
		    pathFinderType = "CUSTOM";
		}
	}
	
	public Robot() {
	    this(null, null, null, null, null);
	}
	
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
		notifyObservers();
	}
	
	public void setTemporarilyBlocked(boolean temporarilyBlocked) {
		this.temporarilyBlocked = temporarilyBlocked;
		notifyObservers();
	}

	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	public int getSpeed() {
		return speed;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}
	
	public Battery getBattery() {
	    return battery;
	}
	
	public Component getCurrTargetComponent() {
	    return currTargetComponent;
	}
	
	public Position getBlockedTargetPosition() {
		return blockedTargetPosition;
	}
	
	public List<Component> getTargetComponents() {
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
	
	public String getPathFinderType() {
	    return pathFinderType;
	}

	public void setPathFinderType(String pathFinderType) {
	    this.pathFinderType = pathFinderType;
	}
	
	private FactoryPathFinder getPathFinder() {
		if (pathFinder == null) {
			if ("JGRAPH".equals(pathFinderType)) {
				pathFinder = new JGraphTDijkstraFactoryPathFinder(getFactory(), getFactory().getPathResolution());
			} else {
				pathFinder = new CustomDijkstraFactoryPathFinder(getFactory(), getFactory().getPathResolution());
			}
		}
		return pathFinder;
	}
	
	@JsonIgnore
	@Override
	public boolean isMobile() {
		return true;
	}
	
	public boolean isBlocked() {
		return blocked;
	}
	
	public boolean isTemporarilyBlocked() {
		return temporarilyBlocked;
	}


	@Override
	public boolean behave() {
		if (getTargetComponents().isEmpty()) {
			return false;
		}
		
		restoreRuntimeState();
		
		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			currTargetComponent = nextTargetComponentToVisit();
			
			LOGGER.info(getName() + " START path with " + getPathFinder().getClass().getSimpleName());
			computePathToCurrentTargetComponent();
			LOGGER.info(getName() + " FINISH path with " + getPathFinder().getClass().getSimpleName());
		}

		return moveToNextPathPosition() != 0;
	}
	
	private void restoreRuntimeState() {
		if (targetComponentsIterator == null) {
			targetComponentsIterator = getTargetComponents().iterator();

			if (currTargetComponent != null) {
				while (targetComponentsIterator.hasNext()) {
					Component component = targetComponentsIterator.next();

//					if (component == currTargetComponent) {
//						break;
//					}
					
					if (component.getClass().equals(currTargetComponent.getClass())
						&& component.getxCoordinate() == currTargetComponent.getxCoordinate()
						&& component.getyCoordinate() == currTargetComponent.getyCoordinate()) {
						break;
					}
				}
			}
		}

		if (currentPathPositionsIter == null && currTargetComponent != null && !hasReachedCurrentTarget()) {
			LOGGER.info(getName() + " START path with " + getPathFinder().getClass().getSimpleName());
			computePathToCurrentTargetComponent();
			LOGGER.info(getName() + " FINISH path with " + getPathFinder().getClass().getSimpleName());
		}
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
		final List<Position> currentPathPositions = getPathFinder().findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
	}
	
	private Motion computeMotion() {
		if (!currentPathPositionsIter.hasNext() && blockedTargetPosition == null) {

			// There is no free path to the target
			setBlocked(true);
			setTemporarilyBlocked(false);
			
			LOGGER.info(getName() + " PERMANENTLY BLOCKED");
			
			return null;
		}
		
		
		final Position targetPosition = getTargetPosition();
	
		// If there is another robot, memorize the blocked target position for the next run
		if (!getFactory().tryGetLockAtPosition(targetPosition.getxCoordinate(), targetPosition.getyCoordinate())) {
			this.blockedTargetPosition = targetPosition;
			
			setTemporarilyBlocked(true);
			
			LOGGER.info(getName() + " TEMPORARILY BLOCKED");
			
			// See if that other robot is permanently blocked and will not move from the target position
			final Component otherComponent = getFactory().getMobileComponentAt(targetPosition, this);

			if (otherComponent instanceof Robot) {
				final Robot otherRobot = (Robot) otherComponent;

				if (otherRobot.isBlocked()) {
					setBlocked(true);
					setTemporarilyBlocked(false);
				}
			}
			return null;
		}
		
		// Reset the memorized position
		this.blockedTargetPosition = null;
		setTemporarilyBlocked(false);
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
	
	@JsonIgnore
	@Override
	public Style getStyle() {
		if (blocked) {
			return BLOCKED_STYLE;
		} else if (temporarilyBlocked) {
			return TEMP_BLOCKED_STYLE;
		} else {
			return STYLE;
		}
	}
	
	@JsonIgnore
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
			LOGGER.log(Level.SEVERE, "The starting position of two robots is the same");
		} else {
			try {
				while(isSimulationStarted()) {
					behave();
					
					try {
						Thread.sleep(250);
					}
					catch (InterruptedException ex) {
						LOGGER.log(Level.WARNING, "Simulation was abruptly interrupted", ex);
					}
				}
			} finally {
				Position currentPosition = getPosition();
				getFactory().unlock(currentPosition.getxCoordinate(), currentPosition.getyCoordinate());
			}
		}
	}
}
