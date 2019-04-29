import React, { useEffect, useState } from 'react';
import InfiniteScroll from 'react-infinite-scroller';

import Card from '../components/Card';

import { get } from '../lib/api';
import { toDate, downloadFile } from '../lib/helpers';

const Log = () => {
	const [logs, setLogs] = useState([]);
	const [offset, setOffset] = useState({ max: 0, min: 0 });

	const getLogs = async () => {
		const params =
			offset.max > 0
				? {
						min: offset.min + logs.length,
						max: Math.min(offset.max, offset.min + logs.length + 30)
				  }
				: null;

		const response = await get('logs', null, params);

		setOffset({ max: response.max, min: response.min });
		setLogs(logs.concat(response.logs));
	};

	useEffect(() => {
		getLogs();
	}, []);

	const parseLog = () => {
		const log = logs.map(({ timestamp, message }) => `${toDate(timestamp, true)} ${message}`);
		return log.join('\n');
	};

	return (
		<section className="log">
			<article>
				<h1>Log</h1>
				<Card>
					<nav className="corner">
						<button
							onClick={() => downloadFile('ict-log.txt', parseLog())}
							className="button success small"
							type="button"
						>
							Export
						</button>
					</nav>
					{logs.length > 0 && (
						<InfiniteScroll
							pageStart={0}
							loadMore={getLogs}
							hasMore={logs.length < offset.max - offset.min}
							loader={<p>Loading ...</p>}
						>
							{logs.map(({ timestamp, level, message }, index) => (
								<p key={`${timestamp}-${index}`} className={level.toLowerCase()}>
									<strong>{toDate(timestamp, true)}</strong>
									<span>{message}</span>
								</p>
							))}
						</InfiniteScroll>
					)}
				</Card>
			</article>
		</section>
	);
};

export default Log;
