package tools.sapcx.commerce.toolkit.testing.itemmodel;

import static tools.sapcx.commerce.toolkit.testing.itemmodel.InMemoryItemModelContext.contextWithAttributes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.model.AbstractItemModel;
import de.hybris.platform.servicelayer.model.ItemModelContext;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;

import org.apache.commons.lang3.LocaleUtils;

public class InMemoryModelFactory {
	private static Locale defaultLocale = Locale.ENGLISH;

	public static <T extends ItemModel> T createTestableItemModel(Class<T> itemType) {
		return createWithContext(itemType, contextWithAttributes(itemType));
	}

	public static <T extends ItemModel> T createTestableItemModel(Class<T> itemType, int typecode) {
		return createWithContext(itemType, contextWithAttributes(itemType, typecode));
	}

	public static <T extends ItemModel> T createTestableItemModel(Class<T> itemType, Map<String, DynamicAttributeHandler> dynamicAttributeHandler) {
		InMemoryItemModelContext context = contextWithAttributes(itemType);
		T item = createWithContext(itemType, context);
		for (Map.Entry<String, DynamicAttributeHandler> handler : dynamicAttributeHandler.entrySet()) {
			addHandlerForDynamicAttribute(item, handler.getKey(), handler.getValue());
		}
		return item;
	}

	public static <VALUE, T extends AbstractItemModel> DynamicAttributeHandler<VALUE, T> handlerWithInitialValue(VALUE value) {
		return new InMemoryDynamicAttributeHandler<>(value);
	}

	public static <T extends ItemModel, VALUE> void addHandlerForDynamicAttribute(T item, String key, DynamicAttributeHandler<VALUE, T> handler) {
		if (!(item.getItemModelContext() instanceof InMemoryItemModelContext)) {
			throw new IllegalArgumentException();
		} else {
			InMemoryItemModelContext context = (InMemoryItemModelContext) item.getItemModelContext();
			context.setDynamicHandler(item, key, handler);
		}
	}

	public static <T extends ItemModel> T copy(T orig) {
		ItemModelContext itemModelContext = orig.getItemModelContext();
		if (!(itemModelContext instanceof InMemoryItemModelContext)) {
			throw new IllegalArgumentException();
		}

		InMemoryItemModelContext context = (InMemoryItemModelContext) itemModelContext;
		InMemoryItemModelContext clonedContext = context.copy();
		return createWithContext((Class<T>) orig.getClass(), clonedContext);
	}

	private static <T extends ItemModel> T createWithContext(Class<T> itemType, InMemoryItemModelContext context) {
		try {
			Constructor<T> constructor = itemType.getConstructor(ItemModelContext.class);
			T instance = constructor.newInstance(context);
			context.updateItemAfterCopy(instance);
			return instance;
		} catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException("Create invoked for unknown item type: " + itemType.getSimpleName());
		}
	}

	public static Locale getDefaultLocale() {
		return defaultLocale;
	}

	public static void setDefaultLocale(String isoCode) {
		defaultLocale = LocaleUtils.toLocale(isoCode);
	}

	public static void setDefaultLocale(Locale locale) {
		defaultLocale = locale;
	}

	public static void resetDefaultLocale() {
		defaultLocale = Locale.ENGLISH;
	}

	public static InMemoryItemModelContextAccessor getContextAccessor(Object o) {
		if (o instanceof AbstractItemModel) {
			AbstractItemModel item = (AbstractItemModel) o;
			ItemModelContext itemModelContext = item.getItemModelContext();
			if (itemModelContext instanceof InMemoryItemModelContextAccessor) {
				return (InMemoryItemModelContextAccessor) itemModelContext;
			}
		}
		throw new UnsupportedOperationException("This method only works with items generated by this factory!");
	}

	static ItemModelAttribute attributeFor(String key, Object value) {
		return new InMemoryItemModelAttribute(key, value);
	}

	static <T> LocalizedAttributeBuilder<T> localizedAttributeFor(String key, Class<T> valueType) {
		return new LocalizedAttributeBuilder(key, valueType);
	}

	static class LocalizedAttributeBuilder<T> {
		private InMemoryLocalizedItemModelAttribute attribute;

		public LocalizedAttributeBuilder(String key, Class<T> valueType) {
			this.attribute = new InMemoryLocalizedItemModelAttribute(key);
		}

		public LocalizedAttributeBuilder withValue(Locale locale, T value) {
			this.attribute.setValue(locale, value);
			return this;
		}

		public ItemModelAttribute build() {
			return this.attribute.clone();
		}
	}
}
