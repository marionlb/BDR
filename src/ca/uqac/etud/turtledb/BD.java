package ca.uqac.etud.turtledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ca.uqac.dim.turtledb.Engine;
import ca.uqac.dim.turtledb.Table;
import ca.uqac.dim.turtledb.VariableTable;

/**
 * Structure de données représentant l'état de la base de données actuelle
 * @author marion
 *
 */
public class BD {

	/**
	 * Liste des tables ({@link Table}), indexées par leur nom
	 */
	final static HashMap<String, Table> tables = new HashMap<String, Table>();
	/**
	 * Liste des sites ({@link Engine}), indexés par leur nom
	 */
	final static HashMap<String, Engine> sites = new HashMap<String, Engine>();

	/**
	 * Répertoire des tables ({@link Table}), classées par nom de site 
	 */
	final static HashMap<String, List<Table>> tablesSites = new HashMap<String, List<Table>>();
	
	/**
	 * Répertoire des sites ({@link Engine}), répertoriés par nom de tables
	 */
	final static HashMap<String, List<Engine>> sitesTables = new HashMap<String, List<Engine>>();

	/**
	 * Tableau des coûts de stockage
	 */
	static HashMap<String, Float> coutsStockage;
	/**
	 * Matrice des coûts de communication
	 */
	static Matrice coutsComm;
	
	public static void addSite(Engine e) {
		sites.put(e.getName(), e);
	}

	public static void addTable(Table t) {
		if(t.getName()==null || t.getName()=="") {
			if(t.getSchema()!=null && t.getSchema().get(0)!=null && t.getSchema().get(0).getTableName()!="")
			tables.put(t.getSchema().get(0).getTableName(), t);
		} else
			tables.put(t.getName(), t);
	}

	public static void hostTable(Table table, Engine site) {
		if (!tables.containsKey(table.getName())) {
			addTable(table);
		}
		if (!sites.containsKey(site.getName())) {
			addSite(site);
		}
		// on indexe la table par rapport � son site
		if (!tablesSites.containsKey(site.getName())) {
			ArrayList<Table> al = new ArrayList<Table>();
			al.add(table);
			tablesSites.put(site.getName(), al);
		} else {
			List<Table> l = tablesSites.get(site.getName());
			l.add(table);
		}
		// on indexe le site par rapport � la table
		if (!sitesTables.containsKey(table.getName())) {
			ArrayList<Engine> al = new ArrayList<Engine>();
			al.add(site);
			sitesTables.put(table.getName(), al);
		} else {
			List<Engine> l = sitesTables.get(table.getName());
			l.add(site);
		}
	}

	public static void hostTable(Table table, String siteName) {
		if (!sites.containsKey(siteName)) {
			System.err.println("Site inconnu : " + siteName);
		} else {
			Engine site = sites.get(siteName);
			hostTable(table, site);
		}
	}
	public static List<Engine> getTableLocations(VariableTable vt)
	{
		return sitesTables.get(vt.getName());
	}

	public static String isHostedOn(Table table) {
		if (!tables.containsKey(table.getName())) {
			System.err.println("Table inconnue : " + table.getName());
		} else if (!sitesTables.containsKey(table.getName())) {
			System.err
			.println("La table est reconnue mais pas h�berg�e sur un site : "
					+ table.getName());
		} else {
			String res = "";
			List<Engine> list = sitesTables.get(table.getName());
			for (int i = 0; i < list.size(); i++) {
				res += list.get(i).getName();
				if (i != list.size() - 1) {
					res += "\n";
				}
			}
			return res;
		}
		return null;
	}

	public static String hosts(String siteName) {
		if (!sites.containsKey(siteName)) {
			System.err.println("Site inconnu : " + siteName);
		} else if (!tablesSites.containsKey(siteName)) {
			System.err.println("Site non utilis� : " + siteName);
		} else {
			String res = "";
			List<Table> list = tablesSites.get(siteName);
			for (int i = 0; i < list.size(); i++) {
				res += list.get(i);
				if (i != list.size() - 1) {
					res += "\n";
				}
			}
			return res;
		}
		return "Error";
	}

	public static void affiche() {
		StringBuilder ps = new StringBuilder();
		ps.append("\n============================================");
		ps.append("\n===================TABLES===================");
		ps.append("\n============================================\n");
		//		for (Iterator<String> iterator = tables.keySet().iterator(); iterator
		//				.hasNext();) {
		//			String tableName = iterator.next();
		for (String tableName : tables.keySet()) {
			List<Engine> listeSites;
			ps.append(tableName + "\n");
			if (!sitesTables.containsKey(tableName)) {

			} else {
				listeSites = sitesTables.get(tableName);
				for (Engine engine : listeSites) {
					ps.append("\t");
					ps.append(engine.getName());
					ps.append("\n");
				}
			}
		}
		ps.append("===========================================");
		ps.append("\n===================SITES===================");
		ps.append("\n===========================================\n");
		for (Iterator<String> iterator = sites.keySet().iterator(); iterator
		.hasNext();) {
			String siteName = iterator.next();
			List<Table> listeTables;
			ps.append(siteName + "\n");
			if (!tablesSites.containsKey(siteName)) {

			} else {
				listeTables = tablesSites.get(siteName);
				for (Table table : listeTables) {
					ps.append("\t");
					ps.append(table.getName());
					ps.append("\n");
				}
			}
		}
		ps.append("===========================================\n");
		ps.append("===========================================\n");
		System.out.println(ps.toString());
	}

	public static Engine getSite(String siteName) {
		if (sites.containsKey(siteName)) {
			return sites.get(siteName);
		}
		return null;
	}

	public static Table getTable(String tableName) {
		if (tables.containsKey(tableName))
			return tables.get(tableName);
		return null;
	}

	public static boolean isATable(VariableTable vt) {
		if (vt == null)
			return false;
		return tables.containsKey(vt.getName());
	}

	public static Table getTable(VariableTable vt) {
		if (vt == null)
			return null;
		return tables.get(vt.getName());
	}
	
	public static void addDefaultCost(String site1) {
		if (coutsStockage == null)
			coutsStockage = new HashMap<String, Float>();
		if (coutsComm == null)
			coutsComm = new Matrice();

		// cout stockage
		coutsStockage.put(site1, (float) 0.01);
		// cout comm
		for (Engine site : BD.sites.values()) {
			if (!site.getName().equals(site1))
				coutsComm.set(site1, site.getName(), 1);
		}
	}
	// public static void hostFragment(VariableTable frag, Table table, Engine
	// site) {
	// if (!tables.containsKey(table.getName())) {
	// addTable(table);
	// }
	// if (!sites.containsKey(site.getName())) {
	// addSite(site);
	// }
	// // on ajoute le fragment dans le r�pertoire des sites
	// if (!fragmentsSites.containsKey(site.getName())) {
	// ArrayList<VariableTable> al = new ArrayList<VariableTable>();
	// al.add(frag);
	// fragmentsSites.put(site.getName(), al);
	// } else {
	// List<VariableTable> l = fragmentsSites.get(site.getName());
	// l.add(frag);
	// }
	// // on ajoute le fragment dans le r�pertoire des tables
	// if (!fragmentsTables.containsKey(table.getName())) {
	// ArrayList<VariableTable> al = new ArrayList<VariableTable>();
	// al.add(frag);
	// fragmentsTables.put(table.getName(), al);
	// } else {
	// List<VariableTable> l = fragmentsTables.get(table.getName());
	// l.add(frag);
	// }
	// }

	// public static String isHostedOn(VariableTable v) {
	// if(!fragmentsSites.containsValue(v)) {
	// return null;
	// }
	// for (Iterator<Entry<String, List<VariableTable>>> iterator =
	// fragmentsSites.entrySet().iterator(); iterator.hasNext();) {
	// Entry<String, List<VariableTable>> entry = iterator.next();
	// List<VariableTable> list = (List<VariableTable>) entry.getValue();
	// String key = entry.getKey();
	//
	// if(list.contains(v)) {
	// return key;
	// }
	// }
	// return null;
	// }

}
