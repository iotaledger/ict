import React, { Component } from 'react';

import Card from '../components/Card';

import { get } from '../lib/api';
import { toDate, downloadFile } from '../lib/helpers';

class Log extends Component {
	state = {
		logs: []
	};

	componentDidMount() {
		this.init();
	}

	init = async () => {
		const { logs } = await get('logs');
		this.setState({ logs });
	};

	parseLog = () => {
		const { logs } = this.state;
		const log = logs.reverse().map(({ timestamp, message }) => `${toDate(timestamp, true)} ${message}`);
		return log.join('\n');
	};

	render() {
		const { logs } = this.state;

		return (
			<section className="log">
				<article>
					<h1>Log</h1>
					<Card>
						<nav className="corner">
							<button
								onClick={() => downloadFile('ict-log.txt', this.parseLog())}
								className="button success small"
								type="button"
							>
								Export
							</button>
						</nav>
						{logs.map(({ timestamp, level, message }) => (
							<p key={`${timestamp}${message}`} className={level.toLowerCase()}>
								<strong>{toDate(timestamp, true)}</strong>
								<span>{message}</span>
							</p>
						))}
					</Card>
				</article>
			</section>
		);
	}
}

export default Log;
