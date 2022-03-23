package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private GameSetup setup;
	private Player mrX;
	private ImmutableList<Player> detectives;


	@Nonnull
	@Override
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {

		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}


	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private Boolean gameOver;
		private Boolean detectiveWins;



		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			List<Player> tempEveryone = new ArrayList<>();
			tempEveryone.add(mrX);
			tempEveryone.addAll(detectives);
			this.everyone = ImmutableList.copyOf(tempEveryone);
			this.gameOver = false;
			this.detectiveWins = false;



			if (mrX == null) {
				throw new NullPointerException("mrX is null");
			}
			if (!mrX.isMrX()) {
				throw new IllegalArgumentException("No MrX");
			}


			for (int i = 0; i < detectives.size(); i++) {        /*iterate through the list of players*/
				if (detectives.get(i).isMrX()) {
					throw new IllegalArgumentException("more than one mrX");
				}

				if (detectives.get(i).tickets().get(ScotlandYard.Ticket.SECRET) > 0) {
					throw new IllegalArgumentException("detective have secret ticket");
				}
				if (detectives.get(i).tickets().get(ScotlandYard.Ticket.DOUBLE) > 0) {
					throw new IllegalArgumentException("detective have double ticket");
				}


				for (int t = i + 1; t < detectives.size(); t++) {
					if (detectives.get(i).piece() == detectives.get(i + 1).piece()) {
						throw new IllegalArgumentException("duplicated detective");
					}
					if (detectives.get(i).location() == detectives.get(i + 1).location()) {
						throw new IllegalArgumentException("duplicated detectives' location");
					}
				}
			}
			if (setup.rounds.isEmpty()) {
				throw new IllegalArgumentException("empty rounds");
			}
			if (setup.graph.nodes().isEmpty()) {
				throw new IllegalArgumentException("Empty graph");
			}

			getAvailableMoves();

			List<Piece> remainingList = new ArrayList<>(this.remaining);
			if (remainingList.get(0).isDetective()){
				if (this.moves.isEmpty()) {
					this.remaining = ImmutableSet.of(mrX.piece());
					getAvailableMoves();
				}
			}

		}

		//function to create a singlemove
		public ImmutableSet<Move> makeSingleMoves(GameSetup setup,
												  List<Player> detectives,
												  Player player,
												  int source){
			Move.SingleMove move;
			final ArrayList<Move> singleMoves = new ArrayList<>();


			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the list of moves to return, move is not valid
				//
				boolean valid = true;  //checks if the move if valid
				for (Player p : detectives) {
					if (p.location() == destination && p.isDetective()) {
						valid = false;
						break;
					}
				}
				if (!valid) continue;

				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of())) {
					// TODO find out if the player has the required tickets
					//  if it does, construct SingleMove and add it the list of moves to return
					if (player.has(t.requiredTicket())) {
						move = new Move.SingleMove(player.piece(), player.location(), t.requiredTicket(), destination);
						singleMoves.add(move);
						if (player.has(ScotlandYard.Ticket.DOUBLE)){
							if (getSetup().rounds.size() > 2){
								singleMoves.addAll(makeDoubleMoves(detectives, player.use(t.requiredTicket()), destination, move));
							}
						}
					}
				}
				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
				if (player.has(ScotlandYard.Ticket.SECRET)) {
					move = new Move.SingleMove(player.piece(), player.location(), ScotlandYard.Ticket.SECRET, destination);
					singleMoves.add(move);
					player.use(ScotlandYard.Ticket.SECRET);
					if (player.has(ScotlandYard.Ticket.DOUBLE)){
						if (getSetup().rounds.size() > 2){
							singleMoves.addAll(makeDoubleMoves(detectives, player, destination, move));
						}
					}
				}
			}

			return ImmutableSet.copyOf(singleMoves);
		}

		// very similiar to makeSingleMoves() but we take in move as parameter for creating a doublemove object.
		public ImmutableSet<Move> makeDoubleMoves(
				List<Player> detectives,
				Player player,
				int source,
				Move move){
			Set<Move> doubleMoves = new HashSet<>();


			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the list of moves to return, move is not valid
				//
				boolean valid = true;  // check if the move is valid
				for (Player p : detectives) {
					if (p.location() == destination && p.isDetective()) {
						valid = false;
						break;
					}
				}
				if (!valid) continue;

				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of())) {
					// TODO find out if the player has the required tickets
					//  if it does, construct SingleMove and add it the list of moves to return
					if (player.has(t.requiredTicket())) {
						Move.DoubleMove moveTogether = new Move.DoubleMove(player.piece(), move.source(), move.tickets().iterator().next(), source, t.requiredTicket(), destination);
						doubleMoves.add(moveTogether);
					}
				}
				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
				if (player.has(ScotlandYard.Ticket.SECRET)) {
					Move.DoubleMove moveTogether = new Move.DoubleMove(player.piece(), move.source(), move.tickets().iterator().next(), source, ScotlandYard.Ticket.SECRET, destination);
					doubleMoves.add(moveTogether);
				}
			}

			return ImmutableSet.copyOf(doubleMoves);
		}




		@Override
		@Nonnull
		public GameSetup getSetup() {
			return setup;
		}

		@Override
		@Nonnull
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> temp = new HashSet();   /*create a new Set<Piece> to add elements*/
			temp.add(mrX.piece());
			for(Player d : detectives){
				temp.add(d.piece());
			}
			remaining = ImmutableSet.copyOf(temp);    /*copy temp Set to remaining*/
			return remaining;
		}

		@Override
		@Nonnull
		public GameState advance(Move move) {

			moves = getAvailableMoves();                //get moves to check later if the move is valid
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			List<Piece> remainingList = new ArrayList<>(remaining);
			ArrayList<LogEntry> tempLog = new ArrayList<>(log);
			ArrayList<Player> tempEveryone =  new ArrayList<>();

			Set<Piece> tempRemaining = new HashSet<>();
			Player currentPlayer;
			Player tempMrX = mrX;
			int index = 0;
			int indexLog = log.size();

			for (Player cp : everyone){
				if(cp.piece() == move.commencedBy()){
					index = everyone.indexOf(cp);       //get the index in everyone
				}
			}
			currentPlayer = everyone.get(index);		//set the current player based on who commenced the move



			if (move.commencedBy().isMrX() && currentPlayer.isMrX()) {   // if current player is mrx

				//update current player's location
				int loc = move.visit(new Move.Visitor<>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				});
				currentPlayer = currentPlayer.at(loc);

				//update log
				tempLog.addAll(move.visit(new Move.Visitor<>() {
					@Override
					public List<LogEntry> visit(Move.SingleMove move) {
						List<LogEntry> lg = new ArrayList<>();
						if (!setup.rounds.get(indexLog)){
							lg.add(LogEntry.hidden(move.ticket));
						}
						if (setup.rounds.get(indexLog)){
							lg.add(LogEntry.reveal(move.ticket, move.destination));
						}

						return lg;
					}

					@Override
					public List<LogEntry> visit(Move.DoubleMove move) {
						List<LogEntry> lg = new ArrayList<>();
						if (!setup.rounds.get(indexLog)){     //if first move false(hidden)
							lg.add(LogEntry.hidden(move.ticket1));
							if (!setup.rounds.get(indexLog + 1)){     //after first hidden move, if 2nd move hidden
								lg.add(LogEntry.hidden(move.ticket2));
							}
							if (setup.rounds.get(indexLog + 1)){
								lg.add(LogEntry.reveal(move.ticket2, move.destination2));  //after first hidden move, if 2nd move reveal
							}
						}
						if (setup.rounds.get(indexLog)){     //if 1st move reveal
							lg.add(LogEntry.reveal(move.ticket1, move.destination1));    //changed, destination2 -> destination1 #######
							if (!setup.rounds.get(indexLog + 1)) {
								lg.add(LogEntry.hidden(move.ticket2));   //after 1st reveal move, 2nd move should be hidden
							}
							if (setup.rounds.get(indexLog + 1)) {
								lg.add(LogEntry.reveal(move.ticket2, move.destination2)); //reveal for second move
							}
						}

						return lg;
					}
				}));

				log = ImmutableList.copyOf(tempLog);

				//use tickets for move
				Iterable<ScotlandYard.Ticket> ticketIterator;
				ticketIterator = move.visit(new Move.Visitor<>() {
					@Override
					public Iterable<ScotlandYard.Ticket> visit(Move.SingleMove move) {
						return move.tickets();
					}

					@Override
					public Iterable<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
						return move.tickets();
					}
				});

				ArrayList<ScotlandYard.Ticket> temp = new ArrayList<>((Collection<? extends ScotlandYard.Ticket>) ticketIterator);

				for (ScotlandYard.Ticket ticket : temp) {
					currentPlayer = currentPlayer.use(ticket);
				}

				//update mrX
				mrX = currentPlayer;
				tempEveryone.addAll(detectives); //removed mrX,
				for (Player x : tempEveryone) {
					tempRemaining.add(x.piece());
				}
				remaining = ImmutableSet.copyOf(tempRemaining);
			}

			if (move.commencedBy().isDetective() && currentPlayer.isDetective()) {    // if current player is detective


				//update current player's location
				int loc = move.visit(new Move.Visitor<>() {
					@Override
					public Integer visit(Move.SingleMove move) {
						return move.destination;
					}

					@Override
					public Integer visit(Move.DoubleMove move) {
						return move.destination2;
					}
				});
				currentPlayer = currentPlayer.at(loc);    //update current player at new location



				//use tickets for move, give ticket to mrX
				Iterable<ScotlandYard.Ticket> ticketIterator;
				ticketIterator = move.visit(new Move.Visitor<>() {
					@Override
					public Iterable<ScotlandYard.Ticket> visit(Move.SingleMove move) {
						return move.tickets();
					}

					@Override
					public Iterable<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
						return move.tickets();
					}
				});

				ArrayList<ScotlandYard.Ticket> temp = new ArrayList<>((Collection<? extends ScotlandYard.Ticket>) ticketIterator);

				for (ScotlandYard.Ticket ticket : temp) {
					currentPlayer = currentPlayer.use(ticket);
					tempMrX = tempMrX.give(ticket);
				}

				//update remaining list, mrX and detectives
				ArrayList<Player> tempDet = new ArrayList<>();
				mrX = tempMrX;
				for (Player d : detectives){
					if (d.piece() == currentPlayer.piece()){
						int n = detectives.indexOf(d);
						//update detective
						for (int ind = 0; ind<n; ind++ ){
							tempDet.add(detectives.get(ind));
						}
						tempDet.add(currentPlayer);
						for (int ind = n+1; ind<detectives.size(); ind++){
							tempDet.add(detectives.get(ind));
						}
					}
				}
				detectives = tempDet;

				//update remaining
				if (remainingList.get(0).isMrX()) {
					for (Piece piece : remainingList) {
						remainingList.remove(piece);
					}
					for (Player det : detectives) {
						remainingList.add(det.piece());
					}
				}
				if (remainingList.get(0).isDetective() && remainingList.size() == 1) {
					remainingList.remove(0);
					remainingList.add(mrX.piece());
				}
				if (remainingList.get(0).isDetective() && remainingList.size() > 1) {
					remainingList.remove(currentPlayer.piece());
				}
				tempRemaining.addAll(remainingList);

				remaining = ImmutableSet.copyOf(tempRemaining);

			}

			return new MyGameState(setup, remaining, log, mrX, detectives);   //use remaining list for which player for current move
		}

		@Override
		@Nonnull
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			/* checks if detective is in the list of detectives */
			int j;
			j = 0;
			for (Player player : detectives) {
				if (detective == player.piece()) {
					j = player.location();
				}
			}
			if (j != 0) {      /*checks if j is updated*/
				return Optional.of(j);
			} else return Optional.empty();
		}

		@Override
		@Nonnull
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player p : everyone) {
				if (p.piece() == piece) { 						//*checks if p matches the param*/
					return Optional.of(ticket -> {              //Lambda expression for TicketBoard interface.
						return p.tickets().getOrDefault(ticket, 0); /*get value of ticket if empty, value = 0*/
					});
				}
			}
			return Optional.empty();
		}

		@Override
		@Nonnull
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		public ImmutableSet<Piece> setWinner() {        //setter method to set winner
			if (!gameOver) {
				winner = ImmutableSet.of();
			}
			if (gameOver && detectiveWins){
				Set<Piece> tempWinner = new HashSet<>();
				for (Player player : detectives){
					tempWinner.add(player.piece());
				}
				winner = ImmutableSet.copyOf(tempWinner);
			}
			if (gameOver && !detectiveWins){
				winner = ImmutableSet.of(mrX.piece());
			}
			return winner;
		}

		@Override
		@Nonnull
		public ImmutableSet<Piece> getWinner() {

			if (gameOver){
				setWinner();
			}
			if (!gameOver) {
				List<Boolean> tixDetEmpty = new ArrayList<>();
				List<Piece> remainingList = new ArrayList<>(remaining);

				for (Player p : detectives) {

					Set<Piece> tempWinner = new HashSet<>();
					boolean tixEmpty = false;

					if (p.location() == mrX.location()) {      //if mrX is captured
						for (Player d : detectives) {
							tempWinner.add(d.piece());
						}
						gameOver = true;
						detectiveWins = true;
						break;
					}

					for (Integer v : p.tickets().values()) {     //check if ticket value is zero
						if (!v.equals(0)) {
							tixEmpty = false;
							break;
						} else tixEmpty = true;
					}

					if (tixEmpty) {
						tixDetEmpty.add(true);
					} else tixDetEmpty.add(false);
				}

				if (gameOver){
					return setWinner();
				}

				for (Boolean b : tixDetEmpty) {//check if all detective tickets are zero

					if (!b) {
						gameOver = false;
						break;
					} else {
						gameOver = true;
						detectiveWins = false;
					}
				}

				if (gameOver){
					return setWinner();
				}

				if (moves.isEmpty() && remainingList.get(0).isMrX()) { //if mrX stuck
					gameOver = true;
					detectiveWins = true;
				}

				if (gameOver){
					return setWinner();
				}

				// if max rounds reached, game over
				if (log.size() >= setup.rounds.size() && remaining.contains(mrX.piece())) {
					gameOver = true;
					detectiveWins = false;
				}

				if (gameOver){
					return setWinner();
				}

				setWinner();
			}
			return winner;
		}

		@Override
		@Nonnull
		public ImmutableSet<Move> getAvailableMoves() {

			Set<Move> tempMove = new HashSet<>();
			List<Piece> tempR = new ArrayList<>(remaining);


			if (tempR.get(0).isMrX()){         //get moves for mrX
				tempMove.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moves = ImmutableSet.copyOf(tempMove);
			}
			if (tempR.get(0).isDetective()){    //get moves for detectives
				for (Piece piece : tempR){
					for (Player p : detectives){
						if (piece == p.piece()){
							tempMove.addAll(makeSingleMoves(setup, detectives, p, p.location()));
						}
					}
				}
				moves = ImmutableSet.copyOf(tempMove);
			}

			if (!getWinner().isEmpty()) {
				moves = ImmutableSet.of();
			}

			return moves;
		}

	}

}