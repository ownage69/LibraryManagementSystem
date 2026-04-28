import { Search } from 'lucide-react';
import { useDeferredValue, useState } from 'react';
import type { SelectOption } from '../../types/api';
import { TextInput } from './TextInput';

interface SearchableSelectProps {
  options: SelectOption[];
  value: string;
  onChange: (value: string) => void;
  hasError?: boolean;
  searchPlaceholder?: string;
  emptyMessage?: string;
}

function matchesOption(option: SelectOption, query: string) {
  const haystack = `${option.label} ${option.hint || ''}`.toLowerCase();
  return haystack.includes(query.toLowerCase());
}

export function SearchableSelect({
  options,
  value,
  onChange,
  hasError = false,
  searchPlaceholder = 'Search options',
  emptyMessage = 'No matching options found.',
}: SearchableSelectProps) {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query.trim());
  const selectedOption = options.find((option) => option.value === value);
  const filteredOptions = options.filter(
    (option) => !option.disabled && (!deferredQuery || matchesOption(option, deferredQuery)),
  );

  return (
    <div className={`search-picker ${hasError ? 'search-picker--error' : ''}`}>
      <div className="search-picker__input">
        <Search size={16} />
        <TextInput
          type="search"
          value={query}
          placeholder={searchPlaceholder}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>

      <div className="search-picker__selected">
        {selectedOption ? (
          <>
            <span>Selected:</span>
            <strong>{selectedOption.label}</strong>
            {selectedOption.hint ? <small>{selectedOption.hint}</small> : null}
          </>
        ) : (
          <span>No option selected yet.</span>
        )}
      </div>

      <div className="search-picker__list" role="radiogroup">
        {filteredOptions.length ? (
          filteredOptions.map((option) => {
            const selected = option.value === value;

            return (
              <button
                key={option.value}
                type="button"
                className={`search-picker__option ${selected ? 'search-picker__option--active' : ''}`}
                role="radio"
                aria-checked={selected}
                onClick={() => {
                  onChange(option.value);
                  setQuery('');
                }}
              >
                <span className="search-picker__radio" aria-hidden="true" />
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
