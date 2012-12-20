/*-------------------------------------------------------------------------
    Simple distributed database engine
    Copyright (C) 2012  Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 -------------------------------------------------------------------------*/
package ca.uqac.dim.turtledb;

import java.util.*;

/**
 * A query plan is a set of query fragments associated to
 * various sites.
 * @author sylvain
 *
 */
public class QueryPlan extends HashMap<String,Set<Relation>>
{
	//1. pourquoi un Set<Relation> ? il n'y en a qu'une � chaque fois
	//2. Ensemble d'op�rations simples (une seule op�ration) ou arbre entier � chaque fois ?
	// si 2 : cmt repr�senter r�sultat interm�diaire ? nouveau fragment VariableTable ?
  /**
   * Dummy UID
   */
  private static final long serialVersionUID = 1L;
  
  //Modif pour prendre en compte les couples site-relation déjà existants
  public void put(String key, Relation r)
  {
	if(this.containsKey(key))
		this.get(key).add(r);
	else {
		Set<Relation> rels = new HashSet<Relation>();
		rels.add(r);
		this.put(key, rels);
	}
  }
  public void put_old(String key, Relation r)
  {
    Set<Relation> rels = new HashSet<Relation>();
    rels.add(r);
    this.put(key, rels);
  }
  public QueryPlan(QueryPlan qp)
  {
	  super(qp);
  }
  public QueryPlan()
  {
	  super();
  }
}
