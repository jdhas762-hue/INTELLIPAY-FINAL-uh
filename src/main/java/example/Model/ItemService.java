package example.Model;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ItemService {

    private final List<Item> catalog;

    public ItemService() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("coffee", "Coffee", 20));
        items.add(new Item("sandwich", "Sandwich", 50));
        items.add(new Item("juice", "Juice", 30));
        items.add(new Item("burger", "Burger", 80));
        items.add(new Item("tea", "Tea", 10));
        this.catalog = Collections.unmodifiableList(items);
    }

    public List<Item> getAllItems() {
        return catalog;
    }

    public Item findById(String id) {
        if (id == null) {
            return null;
        }
        for (Item item : catalog) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }
}


