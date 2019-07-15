package db.mysql;

import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import external.TicketMasterAPI;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MySQLConnection implements DBConnection{
	private Connection conn;
	
	public MySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() {
		// TODO Auto-generated method stub
		if(conn != null) {
			try {
				conn.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}	
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if(conn == null) {
			return;
		}
		try {
			String sql = "INSERT IGNORE INTO history(user_id,item_id) VALUES(?,?)";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for(String itemId : itemIds) {
				stmt.setString(1, userId);
				stmt.setString(2, itemId);
				stmt.execute();
			}				
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if(conn == null) {
			return;
		}
		try {
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for(String itemId : itemIds) {
				stmt.setString(1, userId);
				stmt.setString(2, itemId);
				stmt.execute();
			}				
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		// TODO Auto-generated method stub
		if(conn == null)
			return new HashSet<>();
		
		Set<String> favoriteItemIds = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id=?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			stmt.setString(1, userId);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				favoriteItemIds.add(rs.getString("item_id"));
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItemIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		// TODO Auto-generated method stub
		if(conn == null)
			return new HashSet<Item>();
		
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> ItemIds = getFavoriteItemIds(userId);
		
		//操作数据库就要处理异常
		try {
			String sql = "SELECT * FROM items WHERE item_id=?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for(String itemId : ItemIds) {
				stmt.setString(1, itemId);
				//get result(returned data type is ResultSet)
				ResultSet rs = stmt.executeQuery();
				
				Item.ItemBuilder builder = new Item.ItemBuilder();
				
				while(rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rs.getDouble("distance"));
					builder.setRating(rs.getDouble("rating"));
				}
				favoriteItems.add(builder.build());
				
			}
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		// TODO Auto-generated method stub
		if(conn == null)
			return new HashSet<>();
		Set<String> categories = new HashSet<>();
		
		try {
			String sql = "SELECT category FROM categories WHERE item_id=?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, itemId);
			ResultSet rs = stmt.executeQuery();
			
			while(rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
			
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		// TODO Auto-generated method stub
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		List<Item> items = tmAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		// TODO Auto-generated method stub
		if(conn == null) {
			return;
		}
		
		try {
			// SQL injection
			// Example:
			// SELECT * FROM users WHERE username = '<username>' AND password = '<password>';
			//
			// sql = "SELECT * FROM users WHERE username = '" + username + "'
			//       AND password = '" + password + "'"
			//
			// username: aoweifjoawefijwaoeifj
			// password: 123456' OR '1' = '1
			//
			// SELECT * FROM users WHERE username = 'aoweifjoawefijwaoeifj' AND password = '123456' OR '1' = '1'

			//IGNORE can ignore duplicated primary key
			String sql = "INSERT IGNORE INTO items VALUE(?,?,?,?,?,?,?)";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1,item.getItemId());
			stmt.setString(2, item.getName());
			stmt.setDouble(3, item.getRating());
			stmt.setString(4, item.getAddress());
			stmt.setString(5, item.getImageUrl());
			stmt.setString(6, item.getUrl());
			stmt.setDouble(7, item.getDistance());
			stmt.execute();
			
			sql = "INSERT IGNORE INTO categories VALUES(?,?)";
			stmt = conn.prepareStatement(sql);
			for(String category:item.getCategories()) {
				stmt.setString(1, item.getItemId());
				stmt.setString(2,category);
				stmt.execute();
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getFullname(String userId) {
		if(conn == null)
			return null;
		String name = "";
		try {
			String sql = "SELECT first_name, last_name from users WHERE user_id=?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			if(rs.next()) {
				name= String.join(" ", rs.getString("first_name"),rs.getString("last_name"));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return name;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		if(conn==null)
			return false;
		try {
			String sql = "SELECT user_id from users WHERE user_id=? and password=?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();
			if(rs.next()) {
				return true;
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
