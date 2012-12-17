/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uqac.etud.turtledb;

/**
 *
 * @author fx
 */
class Pair<T0, T1>
{
	T0 first;
	T1 second;
	
	public Pair(T0 first, T1 second)
	{
		this.first = first;
		this.second = second;
	}
	
	public T0 getFirst()
	{
		return first;
	}
	public T1 getSecond()
	{
		return second;
	}
}
