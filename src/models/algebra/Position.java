package models.algebra;

import java.util.ArrayList;
import java.util.List;

public class Position implements Cloneable {
	private ArrayList<Integer> orders = new ArrayList<Integer>();
	
	public Position() {
	}
	
	public Position(ArrayList<Integer> orders) {
		this.orders = orders;
	}
	
	public void addHeadOrder(int order) {
		orders.add(0, order);
	}
	
	public int removeHeadOrder() {
		return orders.remove(0);
	}

	public List<Integer> getOrders() {
		return orders;
	}
	
	public boolean isEmpty() {
		return (orders == null || orders.size() == 0);
	}

	public boolean isAncestorOf(Position another) {
		if (another.orders.size() < this.orders.size()) return false;
		for (int i = 0; i < orders.size(); i++) {
			if (this.orders.get(i) != another.orders.get(i)) return false;
		}
		return true;
	}
	
	public Object clone() {
		return new Position((ArrayList<Integer>) orders.clone());
	}
	
	public boolean equals(Object another) {
		if (!(another instanceof Position)) return false;
		return orders.equals(((Position) another).orders);
	}
	
	public int hashCode() {
		return orders.hashCode();
	}
}
