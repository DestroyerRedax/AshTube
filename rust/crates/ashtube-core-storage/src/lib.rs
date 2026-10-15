use ashtube_core_models::{CoreError, FavoriteItem, HistoryItem};

/// A storage trait defining the contract for persisting favourites, watch history,
/// and local player state. On Android, this contract is implemented by Room / SQLite.
pub trait Storage {
    fn is_favorite(&self, media_id: &str) -> Result<bool, CoreError>;
    fn set_favorite(&mut self, item: FavoriteItem, is_favorite: bool) -> Result<(), CoreError>;
    fn get_favorites(&self) -> Result<Vec<FavoriteItem>, CoreError>;
    fn record_history(&mut self, item: HistoryItem) -> Result<(), CoreError>;
    fn get_history(&self) -> Result<Vec<HistoryItem>, CoreError>;
    fn clear_history(&mut self) -> Result<(), CoreError>;
}

/// A development-only in-memory storage implementation for unit testing
/// and native core verification.
#[derive(Debug, Default)]
pub struct MockStorage {
    favorites: Vec<FavoriteItem>,
    history: Vec<HistoryItem>,
}

impl Storage for MockStorage {
    fn is_favorite(&self, media_id: &str) -> Result<bool, CoreError> {
        Ok(self.favorites.iter().any(|fav| fav.media_id == media_id))
    }

    fn set_favorite(&mut self, item: FavoriteItem, is_favorite: bool) -> Result<(), CoreError> {
        self.favorites.retain(|fav| fav.media_id != item.media_id);
        if is_favorite {
            self.favorites.insert(0, item);
        }
        Ok(())
    }

    fn get_favorites(&self) -> Result<Vec<FavoriteItem>, CoreError> {
        Ok(self.favorites.clone())
    }

    fn record_history(&mut self, item: HistoryItem) -> Result<(), CoreError> {
        self.history.retain(|hist| hist.media_id != item.media_id);
        self.history.insert(0, item);
        Ok(())
    }

    fn get_history(&self) -> Result<Vec<HistoryItem>, CoreError> {
        Ok(self.history.clone())
    }

    fn clear_history(&mut self) -> Result<(), CoreError> {
        self.history.clear();
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::{MockStorage, Storage};
    use ashtube_core_models::{FavoriteItem, HistoryItem};

    #[test]
    fn mock_storage_favorites_lifecycle() {
        let mut storage = MockStorage::default();
        let item = FavoriteItem {
            media_id: "mock-1".into(),
            title: "Ashes of Tomorrow".into(),
        };

        assert!(!storage.is_favorite("mock-1").unwrap());
        storage.set_favorite(item.clone(), true).unwrap();
        assert!(storage.is_favorite("mock-1").unwrap());
        assert_eq!(storage.get_favorites().unwrap().len(), 1);

        storage.set_favorite(item, false).unwrap();
        assert!(!storage.is_favorite("mock-1").unwrap());
        assert!(storage.get_favorites().unwrap().is_empty());
    }

    #[test]
    fn mock_storage_history_lifecycle() {
        let mut storage = MockStorage::default();
        let history_entry = HistoryItem {
            media_id: "mock-1".into(),
            title: "Ashes of Tomorrow".into(),
            position_ms: 120_000,
            duration_ms: 7_200_000,
        };

        storage.record_history(history_entry).unwrap();
        let history = storage.get_history().unwrap();
        assert_eq!(history.len(), 1);
        assert_eq!(history[0].position_ms, 120_000);

        storage.clear_history().unwrap();
        assert!(storage.get_history().unwrap().is_empty());
    }
}
