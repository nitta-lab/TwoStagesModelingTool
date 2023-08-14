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
