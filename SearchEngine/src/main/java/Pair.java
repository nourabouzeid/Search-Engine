public class Pair<A, B, C> {
    private A first;
    private B second;
    private C count;
    private String position;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }
    public Pair(A first, B second,C third, String f) {
        this.first = first;
        this.second = second;
        this.count = third;
        this.position = f;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }
    public C getCount() {
        return count;
    }
    public String getPosition() {
        return position;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public void setSecond(B second) {
        this.second = second;
    }
    public void setCount(C third) {
        this.count = third;
    }
    public void setPosition(String third) {
        this.position = third;
    }
}
