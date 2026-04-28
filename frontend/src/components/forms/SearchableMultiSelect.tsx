import { Search } from 'lucide-react';
import { useDeferredValue, useState } from 'react';
import type { SelectOption } from '../../types/api';
import { TextInput } from './TextInput';

interface SearchableMultiSelectProps {
  options: SelectOption[];
  values: string[];
  onToggle: (value: string) => void;
  searchPlaceholder?: string;
  emptyMessage?: string;
}

function matchesOption(option: SelectOption, query: string) {
  const haystack = `${option.label} ${option.hint || ''}`.toLowerCase();
  return haystack.includes(query.toLowerCase());
}

export function SearchableMultiSelect({
  options,
  values,
  onToggle,
  searchPlaceholder = 'Search options',
  emptyMessage = 'No matching options found.',
}: SearchableMultiSelectProps) {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query.trim());
  const selectedOptions = options.filter((option) => values.includes(option.value));
  const filteredOptions = options.filter(
    (option) => !option.disabled && (!deferredQuery || matchesOption(option, deferredQuery)),
  );

  return (
    <div className="search-picker">
      <div className="search-picker__input">
        <Search size={16} />
        <TextInput
          type="search"
          value={query}
          placeholder={searchPlaceholder}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>

      {selectedOptions.length ? (
        <div className="search-picker__tokens">
          {selectedOptions.map((option) => (
            <button
              key={option.value}
              type="button"
              className="search-picker__token"
              onClick={() => onToggle(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : (
        <div className="search-picker__selected">
          <span>No items selected yet.</span>
        </div>
      )}

      <div className="search-picker__list search-picker__list--grid" role="listbox" aria-multiselectable="true">
        {filteredOptions.length ? (
          filteredOptions.map((option) => {
            const selected = values.includes(option.value);

            return (
              <button
                key={option.value}
                type="button"
                className={`search-picker__option ${selected ? 'search-picker__option--active' : ''}`}
                role="option"
                aria-selected={selected}
                onClick={() => onToggle(option.value)}
              >
                <span className="search-picker__checkbox" aria-hidden="true" />
                <span className="search-picker__option-copy">
                  <strong>{option.label}</strong>
                  {option.hint ? <small>{option.hint}</small> : null}
                </span>
              </button>
            );
          })
        ) : (
          <p className="search-picker__empty">{emptyMessage}</p>
        )}
      </div>
    </div>
  );
}
