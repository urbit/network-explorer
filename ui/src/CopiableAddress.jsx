import React from 'react';
import { Text } from '@tlon/indigo-react';
import { useState } from 'react';

export function CopiableAddress(props) {

  const { address } = props;

  const [copied, setCopied] = useState(false);

  const addr = address ? address.substring(0, 8) + '...' + address.slice(-6) : '';

  return (
    <Text
      title={address}
      color='gray'
      fontSize={0}
      cursor='pointer'
      fontFamily='Source Code Pro !important'
      onClick={() => {
        navigator.clipboard.writeText(address);
        setCopied(true);
        setTimeout(() => setCopied(false), 1000);
      }}
    >
      {copied ? 'Copied!' : addr}
    </Text>
  );
}
