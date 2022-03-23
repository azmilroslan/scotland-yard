package uk.ac.bris.cs.scotlandyard.model;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;

import java.util.*;


/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model{
		Collection c = new ArrayList();
		Iterator it = c.iterator();
		private GameState gameState;
		private Set<Observer> observers; // Set of Observer type

		private MyModel(GameState gameState){   //constructor
			this.observers = new HashSet<>();
			this.gameState = gameState;

		}

		@Override
		@Nonnull
		public Board getCurrentBoard() { return this.gameState; } // return current game board.


		//using Model implementation.

		@Override  //Null or another case
		public void registerObserver(@Nonnull Observer observer){
			if(observer == null) {
				throw new NullPointerException("register null observer");
			}
			if(this.observers.contains(observer)) {
				throw new IllegalArgumentException("More than Once"); //whether it contains observer in param or not.
			}
			this.observers.add(observer); // otherwise register observer.
			Iterator it = observers.iterator();
		}

		@Override //Null or another case
		public void unregisterObserver(@Nonnull Observer observer){
			if(observer == null) {
				throw new NullPointerException("unregister null observer");
			}
			if(!observers.iterator().hasNext()) {
				throw new IllegalArgumentException("unregistered observer previously");    //if don't have next
			}
			this.observers.remove(observer); // otherwise unregister observer
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers(){
			return ImmutableSet.copyOf(this.observers);
		}


		// TODO Advance the model with move, then notify all observers of what what just happened.
		//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
		@Override
		public void chooseMove(@Nonnull Move move){

			Collection c = new ArrayList();
			Iterator it = c.iterator();

			gameState = gameState.advance(move);    //call advance() method

			if(gameState.getWinner().isEmpty()) {    //check if winner is empty

				int i =0;
				while ( i < observers.size()){
					observers.iterator().next().onModelChanged(gameState, Event.MOVE_MADE); //case 1, move is made
					i++;
				}

			}else{                                  // else, winner present
				int i =0;
				do {
					observers.iterator().next().onModelChanged(gameState, Event.GAME_OVER); //case 2, game is over
					i++;
				} while (i < observers.size());
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		return new MyModel(new MyGameStateFactory().build(setup,mrX,detectives));
	}
}