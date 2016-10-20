package parse;

public class Predefined {
    enum State implements IState {
        BAD,
        END
    }

    public static final IState BAD = State.BAD;

    public static final IState END = State.END;

}
